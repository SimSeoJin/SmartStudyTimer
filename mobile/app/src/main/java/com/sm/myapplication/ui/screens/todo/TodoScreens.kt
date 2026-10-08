package com.sm.myapplication.ui.screens.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.myapplication.data.entity.TodoCategory
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.ui.components.AppSwitch
import com.sm.myapplication.ui.components.DateWheelPicker
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.LargeTitle
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.PrimaryButton
import com.sm.myapplication.ui.components.RoundCheck
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SectionHeader
import com.sm.myapplication.ui.components.TimeWheelPicker
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.screens.home.formatClock
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.GroupedBg
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodoListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    viewModel: TodoListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateText = remember(state.today) {
        state.today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NavBar(onBack = onBack, backLabel = "홈")

        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LargeTitle(title = "오늘의 할 일", caption = dateText)

                if (state.activeTodos.isEmpty() && state.completedTodos.isEmpty()) {
                    Spacer(Modifier.height(40.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("아직 할 일이 없어요", style = MaterialTheme.typography.titleMedium, color = LabelPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "아래 버튼으로 오늘 할 일을 추가해 보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LabelSecondary,
                        )
                    }
                }

                if (state.activeTodos.isNotEmpty()) {
                    SectionHeader("진행 중 · ${state.activeTodos.size}")
                    InsetCard {
                        state.activeTodos.forEachIndexed { index, todo ->
                            TodoRow(todo) { viewModel.toggle(todo) }
                            if (index != state.activeTodos.lastIndex) RowDivider(inset = 48.dp)
                        }
                    }
                }

                if (state.completedTodos.isNotEmpty()) {
                    SectionHeader("완료 · ${state.completedTodos.size}")
                    InsetCard {
                        state.completedTodos.forEachIndexed { index, todo ->
                            TodoRow(todo) { viewModel.toggle(todo) }
                            if (index != state.completedTodos.lastIndex) RowDivider(inset = 48.dp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        PrimaryButton(
            text = "＋  추가",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            onClick = onAdd,
        )
    }
}

@Composable
private fun TodoRow(todo: TodoEntity, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundCheck(checked = todo.isDone)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (todo.isDone) LabelSecondary else LabelPrimary,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        if (todo.category != TodoCategory.GENERAL) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AccentTint)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(categoryLabel(todo.category), style = MaterialTheme.typography.labelSmall, color = AccentInk)
            }
            Spacer(Modifier.width(8.dp))
        }
        todo.timeMinutes?.let {
            Text(formatClock(it), style = MaterialTheme.typography.bodySmall, color = LabelTertiary)
        }
    }
}

internal fun categoryLabel(category: TodoCategory): String = when (category) {
    TodoCategory.GENERAL -> "일반"
    TodoCategory.EXAM -> "시험"
    TodoCategory.CERT -> "자격증"
}

internal fun categoryIcon(category: TodoCategory): ImageVector = when (category) {
    TodoCategory.GENERAL -> Icons.Rounded.Article
    TodoCategory.EXAM -> Icons.Rounded.CalendarToday
    TodoCategory.CERT -> Icons.Rounded.WorkspacePremium
}

@Composable
fun TodoAddScreen(
    onBack: () -> Unit,
    viewModel: TodoAddViewModel = viewModel(),
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TodoCategory.GENERAL) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var useTime by remember { mutableStateOf(false) }
    var timeMinutes by remember { mutableStateOf(9 * 60) }

    val save = {
        viewModel.save(title, category, date.toEpochDay(), if (useTime) timeMinutes else null, onDone = onBack)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        NavBar(
            title = "일정 추가",
            onBack = onBack,
            backLabel = "취소",
        )

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader("할 일")
            InsetCard {
                FieldRow(label = "이름", value = title, placeholder = "무엇을 할까요?") { title = it }
            }

            SectionHeader("분류")
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TodoCategory.entries.forEach { c ->
                    CategoryChip(
                        category = c,
                        selected = category == c,
                        modifier = Modifier.weight(1f),
                    ) { category = c }
                }
            }

            SectionHeader("일시")
            InsetCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    DateWheelPicker(date = date, onDateChange = { date = it })
                }
                RowDivider(inset = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("시간 설정", style = MaterialTheme.typography.bodyMedium, color = LabelPrimary, modifier = Modifier.weight(1f))
                    AppSwitch(checked = useTime, onCheckedChange = { useTime = it })
                }
                if (useTime) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                        TimeWheelPicker(minutes = timeMinutes, onMinutesChange = { timeMinutes = it })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = "저장",
                enabled = title.isNotBlank(),
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = save,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    placeholder: String,
    numeric: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = LabelPrimary,
            modifier = Modifier.width(64.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = LabelTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LabelPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: TodoCategory,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentGreen else CardWhite)
            .pressable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = if (selected) CardWhite else AccentInk,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            categoryLabel(category),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CardWhite else LabelSecondary,
        )
    }
}

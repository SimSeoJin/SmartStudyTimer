package com.sm.myapplication.ui.screens.todo

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sm.myapplication.data.entity.TodoCategory
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.BgGray
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.Gray
import com.sm.myapplication.ui.theme.GreenPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodoListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    viewModel: TodoListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateText = remember(state.today) {
        state.today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 헤더 (#F0F7E7, 222dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(BgGreenLight),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                    Spacer(Modifier.weight(1f))
                    Text(dateText, color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp))
                }
                Text(
                    "오늘의 할 일",
                    color = Black50,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 미완료 섹션
            if (state.activeTodos.isNotEmpty()) {
                TodoSection(items = state.activeTodos, onToggle = viewModel::toggle)
                Spacer(Modifier.height(16.dp))
            }
            // 완료 섹션
            if (state.completedTodos.isNotEmpty()) {
                TodoSection(items = state.completedTodos, onToggle = viewModel::toggle)
                Spacer(Modifier.height(16.dp))
            }

            if (state.activeTodos.isEmpty() && state.completedTodos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("아직 할 일이 없어요.\n+ 추가 버튼으로 시작하세요.", color = Black50, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(120.dp))
        }

        // 하단 "추가" 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(GreenPrimary)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Text("추가", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun TodoSection(items: List<TodoEntity>, onToggle: (TodoEntity) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE4E8EC))
            .padding(1.dp),
    ) {
        items.forEachIndexed { index, todo ->
            TodoRow(todo, onToggle = { onToggle(todo) })
            if (index != items.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE4E8EC)))
            }
        }
    }
}

@Composable
private fun TodoRow(todo: TodoEntity, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 카테고리 원형
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BgGreenLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(todo.category),
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B1A1C),
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
            todo.timeMinutes?.let { mins ->
                val h = mins / 60
                val m = mins % 60
                val ampm = if (h < 12) "am" else "pm"
                val h12 = if (h % 12 == 0) 12 else h % 12
                Text(
                    text = "%d:%02d%s".format(h12, m, ampm),
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                )
            }
        }

        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (todo.isDone) GreenPrimary else Color.White)
                .border(1.5.dp, if (todo.isDone) GreenPrimary else Gray, RoundedCornerShape(3.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (todo.isDone) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Black50, modifier = Modifier.size(22.dp))
    }
}

private fun categoryIcon(category: TodoCategory): ImageVector = when (category) {
    TodoCategory.GENERAL -> Icons.Outlined.Article
    TodoCategory.EXAM -> Icons.Outlined.CalendarToday
    TodoCategory.CERT -> Icons.Outlined.EmojiEvents
}

@Composable
fun TodoAddScreen(
    onBack: () -> Unit,
    viewModel: TodoAddViewModel = viewModel(),
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TodoCategory.GENERAL) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))) }
    var timeText by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 헤더 (#F0F7E7, 96dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(BgGreenLight),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(icon = Icons.Filled.Close, onClick = onBack)
                    Spacer(Modifier.weight(1f))
                    Text("일정 추가", color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(40.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // 일정 이름
            FieldLabel("일정 이름")
            TextInput(value = title, placeholder = "Plan Name", onValueChange = { title = it })

            Spacer(Modifier.height(20.dp))

            // 분류
            FieldLabel("분류")
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CategoryChip(TodoCategory.GENERAL, selected = category == TodoCategory.GENERAL) { category = TodoCategory.GENERAL }
                CategoryChip(TodoCategory.EXAM, selected = category == TodoCategory.EXAM) { category = TodoCategory.EXAM }
                CategoryChip(TodoCategory.CERT, selected = category == TodoCategory.CERT) { category = TodoCategory.CERT }
            }

            Spacer(Modifier.height(20.dp))

            // 날짜 + 시간 (좌우 분할)
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("날짜", color = Black50, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                    TextInputBox(value = dateText, placeholder = "YYYY/MM/DD", trailingIcon = Icons.Filled.CalendarMonth) { dateText = it }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("시간", color = Black50, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                    TextInputBox(value = timeText, placeholder = "Time", trailingIcon = Icons.Filled.AccessTime) { timeText = it }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 메모
            FieldLabel("메모")
            TextInput(value = memo, placeholder = "Notes", onValueChange = { memo = it })

            Spacer(Modifier.height(120.dp))
        }

        // 저장 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(GreenPrimary)
                .clickable {
                    val day = runCatching {
                        LocalDate.parse(dateText, DateTimeFormatter.ofPattern("yyyy/MM/dd")).toEpochDay()
                    }.getOrDefault(LocalDate.now().toEpochDay())
                    val minutes = parseTimeMinutes(timeText)
                    viewModel.save(title, category, day, minutes, memo, onDone = onBack)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("저장", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Black50,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
    )
}

@Composable
private fun TextInput(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    TextInputBox(value = value, placeholder = placeholder, trailingIcon = Icons.Filled.MoreVert, onValueChange = onValueChange)
}

@Composable
private fun TextInputBox(
    value: String,
    placeholder: String,
    trailingIcon: ImageVector,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(BgGray)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = Color(0xFF9E9E9E), fontSize = 15.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(end = 28.dp),
            singleLine = true,
        )
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(18.dp).align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun CategoryChip(category: TodoCategory, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) GreenPrimary else BgGreenLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.name,
            tint = if (selected) Color.White else GreenPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun parseTimeMinutes(text: String): Int? {
    if (text.isBlank()) return null
    val regex = Regex("""(\d{1,2})\s*:?\s*(\d{1,2})?""")
    val m = regex.find(text) ?: return null
    val h = m.groupValues[1].toIntOrNull() ?: return null
    val min = m.groupValues[2].toIntOrNull() ?: 0
    if (h !in 0..23 || min !in 0..59) return null
    return h * 60 + min
}

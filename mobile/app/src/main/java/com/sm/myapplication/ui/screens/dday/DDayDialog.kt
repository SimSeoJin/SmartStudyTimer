package com.sm.myapplication.ui.screens.dday

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.PrimaryButton
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.FillGray
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DDayDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context.applicationContext as Application) }
    val savedDay by repo.ddayEpochDay.collectAsState(initial = null)
    val savedLabel by repo.ddayLabel.collectAsState(initial = "기말 고사")
    val scope = rememberCoroutineScope()

    var dd by remember { mutableStateOf("") }
    var mm by remember { mutableStateOf("") }
    var yyyy by remember { mutableStateOf("") }
    var label by remember { mutableStateOf(savedLabel) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedDay, savedLabel) {
        savedDay?.let {
            val d = LocalDate.ofEpochDay(it)
            dd = d.dayOfMonth.toString().padStart(2, '0')
            mm = d.monthValue.toString().padStart(2, '0')
            yyyy = d.year.toString()
        }
        label = savedLabel
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite)
                .imePadding(),
        ) {
            NavBar(title = "디데이 설정", actionLabel = "닫기", onAction = onClose)

            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                Text("이름", style = MaterialTheme.typography.labelMedium, color = LabelSecondary)
                Spacer(Modifier.height(6.dp))
                FilledField(
                    value = label,
                    placeholder = "예: 기말 고사",
                    modifier = Modifier.fillMaxWidth(),
                ) { label = it }

                Spacer(Modifier.height(14.dp))

                Text("날짜", style = MaterialTheme.typography.labelMedium, color = LabelSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledField(
                        value = yyyy,
                        placeholder = "YYYY",
                        numeric = true,
                        center = true,
                        modifier = Modifier.weight(1.4f),
                    ) { yyyy = it.take(4).filter(Char::isDigit) }
                    FilledField(
                        value = mm,
                        placeholder = "MM",
                        numeric = true,
                        center = true,
                        modifier = Modifier.weight(1f),
                    ) { mm = it.take(2).filter(Char::isDigit) }
                    FilledField(
                        value = dd,
                        placeholder = "DD",
                        numeric = true,
                        center = true,
                        modifier = Modifier.weight(1f),
                    ) { dd = it.take(2).filter(Char::isDigit) }
                }

                errorMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Destructive)
                }

                Spacer(Modifier.height(18.dp))

                PrimaryButton(text = "저장") {
                    val y = yyyy.toIntOrNull()
                    val m = mm.toIntOrNull()
                    val d = dd.toIntOrNull()
                    if (y == null || m == null || d == null) {
                        errorMsg = "년·월·일을 모두 입력해 주세요"
                        return@PrimaryButton
                    }
                    val date = runCatching { LocalDate.of(y, m, d) }.getOrNull()
                    if (date == null) {
                        errorMsg = "올바르지 않은 날짜예요"
                        return@PrimaryButton
                    }
                    errorMsg = null
                    scope.launch {
                        repo.setDDay(date.toEpochDay(), label.ifBlank { "기말 고사" })
                        onClose()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilledField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    center: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FillGray)
            .padding(horizontal = 12.dp),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = LabelTertiary,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = LabelPrimary,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

package com.sm.myapplication.ui.screens.dday

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.GreenPrimary
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

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Black50, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("디데이 수정", color = Black50, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(36.dp))
            }

            Spacer(Modifier.height(16.dp))

            // 라벨 입력
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgGreenLight)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (label.isEmpty()) Text("디데이 이름 (예: 기말 고사)", color = Color(0xFF9E9E9E), fontSize = 14.sp)
                BasicTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp, color = Black50),
                )
            }

            Spacer(Modifier.height(12.dp))

            // DD MM YYYY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateNumberBox(value = yyyy, placeholder = "YYYY", modifier = Modifier.weight(1.4f)) { yyyy = it.take(4).filter(Char::isDigit) }
                DateNumberBox(value = mm, placeholder = "MM", modifier = Modifier.weight(1f)) { mm = it.take(2).filter(Char::isDigit) }
                DateNumberBox(value = dd, placeholder = "DD", modifier = Modifier.weight(1f)) { dd = it.take(2).filter(Char::isDigit) }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMsg!!,
                    color = Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 확인 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenPrimary)
                    .clickable {
                        val y = yyyy.toIntOrNull()
                        val m = mm.toIntOrNull()
                        val d = dd.toIntOrNull()
                        if (y == null || m == null || d == null) {
                            errorMsg = "년/월/일을 모두 입력하세요"
                            return@clickable
                        }
                        val date = runCatching { LocalDate.of(y, m, d) }.getOrNull()
                        if (date == null) {
                            errorMsg = "올바르지 않은 날짜입니다"
                            return@clickable
                        }
                        errorMsg = null
                        scope.launch {
                            repo.setDDay(date.toEpochDay(), label.ifBlank { "기말 고사" })
                            onClose()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("확인", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun DateNumberBox(value: String, placeholder: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (value.isEmpty()) Text(placeholder, color = Color(0xFF9E9E9E), fontSize = 14.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(fontSize = 14.sp, color = Black50, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

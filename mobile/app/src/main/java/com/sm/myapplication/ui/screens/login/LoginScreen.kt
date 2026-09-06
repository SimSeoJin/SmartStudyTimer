package com.sm.myapplication.ui.screens.login

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.BgGreenTint
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.KakaoYellow

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    val uiState by viewModel.state

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 헤더
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
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Black50)
                }
                Spacer(Modifier.weight(1f))
                Text("로그인", color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        // ID 입력
        FieldLabel("ID")
        LineInput(value = id, placeholder = "ID를 입력하세요", onValueChange = { id = it })

        Spacer(Modifier.height(18.dp))

        // Password 입력
        FieldLabel("Password")
        LineInput(value = pw, placeholder = "Password를 입력하세요", isPassword = true, onValueChange = { pw = it })

        Spacer(Modifier.height(22.dp))

        // Log In 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgGreenTint)
                .border(1.dp, BgGreenLight, RoundedCornerShape(10.dp))
                .clickable {
                    if (!uiState.isLoading) {
                        viewModel.login(id, pw, onSuccess = onLoginSuccess)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (uiState.isLoading) "로그인 중..." else "Log In",
                color = Black50,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "아이디/비밀번호 찾기",
            color = Black50,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { /* TODO: 아이디 찾기 */ },
        )

        Spacer(Modifier.weight(1f))

        // 카카오 로그인
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KakaoYellow)
                .clickable { onLoginSuccess() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF3C1E1E), modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("카카오계정으로 로그인", color = Color(0xFF3C1E1E), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Black50,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
    )
}

@Composable
private fun LineInput(
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = Color(0xFF9E9E9E), fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E5E5)))
    }
}

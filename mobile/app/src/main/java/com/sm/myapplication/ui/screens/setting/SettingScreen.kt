package com.sm.myapplication.ui.screens.setting

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.Black50

@Composable
fun SettingScreen(onLogout: () -> Unit) {
    val items = listOf(
        SettingItem(Icons.Outlined.Person, "나의 정보"),
        SettingItem(Icons.Outlined.Notifications, "알림"),
        SettingItem(Icons.Outlined.Lock, "개인정보"),
        SettingItem(Icons.Outlined.CreditCard, "결제내역"),
        SettingItem(Icons.AutoMirrored.Outlined.HelpOutline, "도움말"),
        SettingItem(Icons.AutoMirrored.Outlined.Logout, "로그아웃", onLogout),
    )

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
                Text("설정", color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { it ->
                SettingMenuRow(it)
            }
        }
    }
}

private data class SettingItem(
    val icon: ImageVector,
    val label: String,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun SettingMenuRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.onClick != null) { item.onClick?.invoke() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, null, tint = Black50, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(16.dp))
        Text(item.label, fontSize = 16.sp, color = Black50, fontWeight = FontWeight.Medium)
    }
}

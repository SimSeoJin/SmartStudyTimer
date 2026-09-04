package com.sm.myapplication.ui.nav

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.GreenDark
import com.sm.myapplication.ui.theme.GreenPrimary

data class BottomTab(
    val route: String,
    val label: String,
)

val bottomTabs = listOf(
    BottomTab(Route.HOME, "Home"),
    BottomTab(Route.CALENDAR, "Calender"),
    BottomTab(Route.RANK, "Rank"),
    BottomTab(Route.SETTING, "Setting"),
)

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    // 바깥: 흰색 (메인 화면 배경과 동일)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 안쪽: 흰색 알약 컨테이너 (그림자로 입체감)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(50),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                bottomTabs.forEach { tab ->
                    BottomTabItem(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onTabSelected(tab.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pillBg = if (selected) Color(0x4DC8EDC8) else Color.Transparent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DiamondIcon(big = selected)
            Spacer(Modifier.height(3.dp))
            Text(
                text = tab.label,
                color = if (selected) GreenDark else GreenPrimary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun DiamondIcon(big: Boolean) {
    val side = if (big) 18.dp else 14.dp
    Box(
        modifier = Modifier
            .size(side)
            .rotate(45f)
            .clip(RoundedCornerShape(2.dp))
            .background(GreenPrimary),
    )
}

package com.sm.myapplication.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.Separator

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

val bottomTabs = listOf(
    BottomTab(Route.HOME, "홈", Icons.Outlined.Home, Icons.Rounded.Home),
    BottomTab(Route.CALENDAR, "캘린더", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
    BottomTab(Route.RANK, "랭킹", Icons.Outlined.EmojiEvents, Icons.Rounded.EmojiEvents),
    BottomTab(Route.SETTING, "설정", Icons.Outlined.Settings, Icons.Rounded.Settings),
)

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Separator)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bottomTabs.forEach { tab ->
                BottomTabItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { onTabSelected(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) AccentGreen else LabelSecondary,
        label = "tabTint",
    )
    Column(
        modifier = modifier
            .height(52.dp)
            .pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.iconSelected else tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(25.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            color = tint,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}

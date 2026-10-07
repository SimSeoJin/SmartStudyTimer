package com.sm.myapplication.ui.screens.rank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.myapplication.ui.components.AppSwitch
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SummaryHeader
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Green300
import com.sm.myapplication.ui.theme.Green500
import com.sm.myapplication.ui.theme.Green700
import com.sm.myapplication.ui.theme.Green900
import com.sm.myapplication.ui.theme.GroupedBg
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary

// TODO(서버 연동): 랭킹 API가 붙기 전까지 쓰는 표시용 더미 데이터
private data class RankRow(val rank: Int, val name: String, val time: String, val isMe: Boolean = false)
private data class TopRanker(val rank: Int, val name: String, val time: String, val barHeight: Int)

private val FILTERS = listOf("친구", "전체", "고1", "고2", "고3", "대학생")
private val DUMMY_TOP3 = listOf(
    TopRanker(2, "김철수", "12h 40m", 62),
    TopRanker(1, "이서연", "15h 02m", 88),
    TopRanker(3, "김영희", "11h 15m", 50),
)
private val DUMMY_LIST = listOf(
    RankRow(4, "이연주", "11:11:11"),
    RankRow(5, "박서준", "10:48:02"),
    RankRow(6, "정하늘", "09:30:44"),
    RankRow(7, "최민지", "08:57:10"),
    RankRow(55, "나", "01:24:36", isMe = true),
)
private const val MY_RANK = 55

@Composable
fun RankScreen(onOpenTier: () -> Unit) {
    var pureModeOnly by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SummaryHeader(
            summary = "이번 주 나의 순위",
            highlight = "${MY_RANK}위",
            sub = "${FILTERS[selectedFilter]} 기준 · 티어는 다이아",
            trailing = {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AccentTint)
                        .pressable(onClick = onOpenTier)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        "티어표",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AccentInk,
                    )
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            FILTERS.forEachIndexed { index, name ->
                FilterChip(label = name, selected = selectedFilter == index) { selectedFilter = index }
            }
        }

        Spacer(Modifier.height(20.dp))

        Podium(DUMMY_TOP3)

        Spacer(Modifier.height(16.dp))

        InsetCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable { pureModeOnly = !pureModeOnly }
                    .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "순공 모드만 보기",
                    style = MaterialTheme.typography.titleSmall,
                    color = LabelPrimary,
                    modifier = Modifier.weight(1f),
                )
                AppSwitch(checked = pureModeOnly, onCheckedChange = { pureModeOnly = it })
            }
        }

        Spacer(Modifier.height(10.dp))

        InsetCard {
            DUMMY_LIST.forEachIndexed { index, row ->
                RankListRow(row)
                if (index != DUMMY_LIST.lastIndex) RowDivider(inset = 16.dp)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) AccentGreen else CardWhite)
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) CardWhite else LabelSecondary,
        )
    }
}

@Composable
private fun Podium(top3: List<TopRanker>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        top3.forEach { ranker ->
            PodiumColumn(ranker, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumColumn(ranker: TopRanker, modifier: Modifier = Modifier) {
    val isFirst = ranker.rank == 1
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isFirst) {
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = "1위",
                tint = Color(0xFFF5B301),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .size(if (isFirst) 54.dp else 46.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Green300, AccentGreen)))
                .then(if (isFirst) Modifier.border(2.dp, AccentGreen, CircleShape) else Modifier),
        )
        Spacer(Modifier.height(6.dp))
        Text(ranker.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = LabelPrimary)
        Text(ranker.time, style = MaterialTheme.typography.labelSmall, color = LabelSecondary)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ranker.barHeight.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        if (isFirst) listOf(Green500, Green900) else listOf(Green500, Green700)
                    )
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = "${ranker.rank}",
                style = MaterialTheme.typography.titleMedium,
                color = CardWhite,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RankListRow(row: RankRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (row.isMe) AccentTint else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${row.rank}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (row.isMe) AccentInk else LabelSecondary,
            modifier = Modifier.width(26.dp),
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (row.isMe) Brush.linearGradient(listOf(AccentGreen, AccentInk))
                    else Brush.linearGradient(listOf(Color(0xFFE3E4E8), Color(0xFFD5D7DD)))
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
            ),
            color = LabelPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.time,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (row.isMe) AccentInk else LabelSecondary,
        )
    }
}

@Composable
fun TierRankDialog(onClose: () -> Unit) {
    val tiers = listOf(
        Triple("공부 신", "하루 평균 10시간 이상", Green900),
        Triple("공부 마스터", "하루 평균 7시간 이상", Green700),
        Triple("공부중독", "하루 평균 5시간 이상", AccentGreen),
        Triple("공부 입문", "하루 평균 3시간 이상", Green300),
        Triple("공부 새싹", "하루 평균 1시간 이상", Color(0xFFC9E9D4)),
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite),
        ) {
            NavBar(title = "티어 기준표", actionLabel = "닫기", onAction = onClose)
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tiers.forEach { (name, range, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
                            Text(range, style = MaterialTheme.typography.bodySmall, color = LabelSecondary)
                        }
                    }
                }
            }
        }
    }
}

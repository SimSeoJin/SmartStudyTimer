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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.myapplication.ui.components.AppSwitch
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.DialogHeader
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SummaryHeader
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.theme.PartyRed
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.PartyTint
import com.sm.myapplication.ui.theme.Paper
import com.sm.myapplication.ui.theme.IconSlate
import com.sm.myapplication.ui.theme.IconUmber
import com.sm.myapplication.ui.theme.Ink
import com.sm.myapplication.ui.theme.PaperDeep
import com.sm.myapplication.ui.theme.PosterShape
import com.sm.myapplication.ui.theme.Separator
import com.sm.myapplication.ui.theme.PaperShade
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary

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
            .background(PaperShade)
            .verticalScroll(rememberScrollState()),
    ) {
        SummaryHeader(
            summary = "이번 주 나의 서열",
            highlight = "${MY_RANK}위",
            sub = "${FILTERS[selectedFilter]} 기준 · 계급은 모범 당원",
            trailing = {
                Box(
                    modifier = Modifier
                        .clip(PosterShape.Small)
                        .background(PartyTint)
                        .pressable(onClick = onOpenTier)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        "계급표",
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
            .clip(PosterShape.Small)
            .background(if (selected) PartyRed else Paper)
            .border(1.dp, if (selected) PartyRed else Separator, PosterShape.Small)
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Paper else LabelSecondary,
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
                tint = PartyRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .size(if (isFirst) 54.dp else 46.dp)
                .clip(PosterShape.Small)
                .background(Ink)
                .then(if (isFirst) Modifier.border(2.dp, PartyRed, PosterShape.Small) else Modifier),
        )
        Spacer(Modifier.height(6.dp))
        Text(ranker.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = LabelPrimary)
        Text(ranker.time, style = MaterialTheme.typography.labelSmall, color = LabelSecondary)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ranker.barHeight.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(if (isFirst) PartyRed else Ink),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = "${ranker.rank}",
                style = MaterialTheme.typography.titleMedium,
                color = Paper,
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
            .background(if (row.isMe) PartyTint else Color.Transparent)
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
                .clip(PosterShape.Small)
                .background(if (row.isMe) PartyRed else PaperDeep)
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
        Triple("순공 영웅", "하루 평균 10시간 이상", PartyRed),
        Triple("모범 당원", "하루 평균 7시간 이상", Ink),
        Triple("성실 당원", "하루 평균 5시간 이상", IconUmber),
        Triple("예비 당원", "하루 평균 3시간 이상", IconSlate),
        Triple("일반 동지", "하루 평균 1시간 이상", LabelTertiary),
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(PosterShape.Large)
                .background(Paper),
        ) {
            DialogHeader(title = "계급 기준표", onClose = onClose)
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tiers.forEach { (name, range, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(PosterShape.Small)
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

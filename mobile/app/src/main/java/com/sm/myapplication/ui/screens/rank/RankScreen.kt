package com.sm.myapplication.ui.screens.rank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableFloatStateOf
import com.sm.myapplication.network.dto.tierDisplayName
import com.sm.myapplication.data.repository.AppRepository
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.DialogHeader
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SummaryHeader
import com.sm.myapplication.ui.components.TierBadge
import com.sm.myapplication.ui.components.formatStudyDuration
import com.sm.myapplication.ui.components.tierBadgeRes
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
import com.sm.myapplication.ui.theme.LabelTertiary

private data class RankRow(val rank: Long, val name: String, val time: String, val tier: String?, val isMe: Boolean = false)
private data class TopRanker(val rank: Long, val name: String, val time: String, val tier: String?, val barHeight: Int)

/** 목록에 보여줄 최대 순위(4위~). 내가 그 밖이면 맨 아래에 따로 붙인다. */
private const val LIST_LIMIT = 30

private sealed interface RankLoad {
    data object Loading : RankLoad
    data object Failed : RankLoad
    data class Loaded(val snapshot: AppRepository.RankingSnapshot) : RankLoad
}

/** 초 → "32시간 35분" (앱 공용 표기. 시계 시각처럼 보이는 "32:35:00" 대신) */
private fun formatRankTime(seconds: Long): String = formatStudyDuration(seconds * 1_000L)

@Composable
fun RankScreen(onOpenTier: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context.applicationContext) }
    var load by remember { mutableStateOf<RankLoad>(RankLoad.Loading) }

    LaunchedEffect(Unit) {
        load = repo.fetchRanking()?.let { RankLoad.Loaded(it) } ?: RankLoad.Failed
    }

    val snapshot = (load as? RankLoad.Loaded)?.snapshot
    val all = snapshot?.all.orEmpty()
    val me = all.firstOrNull { it.memberId == snapshot?.myMemberId }

    val sub = when {
        load is RankLoad.Loading -> "랭킹을 불러오는 중…"
        load is RankLoad.Failed -> "랭킹을 불러오지 못했어요"
        me != null -> "최근 7일 나의 순위 ${me.rankPosition}위 · 티어 ${tierDisplayName(me.tierName)}"
        else -> "최근 7일 순공 기록이 없어 아직 순위가 없어요"
    }

    // 하단 고정 '내 순위' 줄: 목록 속 내 줄이 화면 아래로 벗어나 있을 때만 보인다.
    // 스크롤해서 내 줄이 고정 줄 위치까지 올라오면 고정 줄을 숨겨, 목록과 함께 위로 올라가는 것처럼 보이게 한다.
    var viewportBottom by remember { mutableFloatStateOf(0f) }
    var myRowBottom by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val pinnedGapPx = with(LocalDensity.current) { PINNED_BOTTOM_GAP.toPx() }

    // 목록: 4위 ~ LIST_LIMIT위. 내가 그 밖이면 '⋯' 다음에 내 줄을 붙인다.
    val listRows = all.drop(3).take(LIST_LIMIT - 3).map {
        RankRow(it.rankPosition, it.nickname, formatRankTime(it.totalTime), it.tierName, isMe = it.memberId == me?.memberId)
    }
    val myRow = me?.let { RankRow(it.rankPosition, it.nickname, formatRankTime(it.totalTime), it.tierName, isMe = true) }
    val meOutsideList = me != null && all.indexOf(me) >= LIST_LIMIT
    val meInList = meOutsideList || listRows.any { it.isMe }
    val showPinned = myRow != null && meInList && viewportBottom > 0f &&
        myRowBottom > viewportBottom - pinnedGapPx + 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .onGloballyPositioned { viewportBottom = it.boundsInWindow().bottom },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SummaryHeader(
                summary = "랭킹",
                sub = sub,
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

            if (snapshot != null && all.isEmpty()) {
                Text(
                    "최근 7일 동안 순공 기록이 있는 사람이 아직 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            }

            if (all.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                // 시상대는 2위-1위-3위 순서로 배치
                val barHeights = listOf(88, 62, 50)
                val top3 = all.take(3).mapIndexed { i, r ->
                    TopRanker(r.rankPosition, r.nickname, formatRankTime(r.totalTime), r.tierName, barHeights[i])
                }
                // 2·3위가 없어도 자리를 비워 둬서 시상대 모양을 유지한다
                Podium(second = top3.getOrNull(1), first = top3[0], third = top3.getOrNull(2))

                Spacer(Modifier.height(16.dp))

                // 사람이 적어 목록이 비면, 빈 공간에 랭킹 규칙을 안내해 순공모드를 유도한다
                if (listRows.isEmpty() && !meOutsideList) {
                    InsetCard {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = Green700,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "순공모드로 공부하면 순위에 올라가요",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = LabelPrimary,
                                )
                                Text(
                                    "최근 7일 순공 시간으로 순위와 티어가 정해져요. 일반 타이머 기록은 반영되지 않아요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LabelSecondary,
                                )
                            }
                        }
                    }
                }

                val trackMyRow = Modifier.onGloballyPositioned { myRowBottom = it.positionInWindow().y + it.size.height }
                if (listRows.isNotEmpty() || meOutsideList) {
                    InsetCard {
                        listRows.forEachIndexed { index, row ->
                            RankListRow(row, if (row.isMe) trackMyRow else Modifier)
                            if (index != listRows.lastIndex || meOutsideList) RowDivider(inset = 16.dp)
                        }
                        if (meOutsideList && myRow != null) {
                            EllipsisRow()
                            RowDivider(inset = 16.dp)
                            RankListRow(myRow, trackMyRow)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showPinned && myRow != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = PINNED_BOTTOM_GAP)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite),
            ) {
                RankListRow(myRow)
            }
        }
    }
}

/** 하단 고정 '내 순위' 줄과 화면 아래 끝 사이 간격 */
private val PINNED_BOTTOM_GAP = 8.dp

/** 30위와 내 순위 사이가 생략됐음을 보여주는 줄 */
@Composable
private fun EllipsisRow() {
    Text(
        text = "⋯",
        style = MaterialTheme.typography.titleMedium,
        color = LabelSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/** 2위-1위-3위 순서로 배치. 비어 있는 순위는 빈 단상으로 보여준다. */
@Composable
private fun Podium(second: TopRanker?, first: TopRanker, third: TopRanker?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(2L to second, 1L to first, 3L to third).forEach { (rank, ranker) ->
            if (ranker != null) {
                PodiumColumn(ranker, modifier = Modifier.weight(1f))
            } else {
                EmptyPodiumColumn(rank, barHeight = if (rank == 2L) 62 else 50, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyPodiumColumn(rank: Long, barHeight: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(barHeight.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(AccentTint),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            color = LabelTertiary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PodiumColumn(ranker: TopRanker, modifier: Modifier = Modifier) {
    val isFirst = ranker.rank == 1L
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
        val avatarSize = if (isFirst) 54.dp else 46.dp
        if (tierBadgeRes(ranker.tier) != null) {
            TierBadge(ranker.tier, size = avatarSize)
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Green300, AccentGreen)))
                    .then(if (isFirst) Modifier.border(2.dp, AccentGreen, CircleShape) else Modifier),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            ranker.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LabelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
private fun RankListRow(row: RankRow, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
        if (tierBadgeRes(row.tier) != null) {
            TierBadge(row.tier, size = 26.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (row.isMe) Brush.linearGradient(listOf(AccentGreen, AccentInk))
                        else Brush.linearGradient(listOf(Color(0xFFE3E4E8), Color(0xFFD5D7DD)))
                    )
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
            ),
            color = LabelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
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
    // 서버 tier_meta(Diamond 4 / Platinum 15 / Gold 45 / Silver 80 / Bronze 100)와 같은 기준.
    // 최근 7일 공부시간 순위의 상위 % 로 정해지며, 최근 7일 기록이 없으면 랭킹·티어에서 제외된다.
    val tiers = listOf(
        "Diamond" to "상위 4% 이내",
        "Platinum" to "상위 15% 이내",
        "Gold" to "상위 45% 이내",
        "Silver" to "상위 80% 이내",
        "Bronze" to "그 외 (최근 7일 순공 기록이 있는 회원)",
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite),
        ) {
            DialogHeader(title = "티어 기준표", onClose = onClose)
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tiers.forEach { (tier, range) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TierBadge(tier, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(tierDisplayName(tier), style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
                            Text(range, style = MaterialTheme.typography.bodySmall, color = LabelSecondary)
                        }
                    }
                }
            }
        }
    }
}

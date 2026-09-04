package com.sm.myapplication.ui.screens.rank

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.myapplication.ui.theme.BgGreenLight
import com.sm.myapplication.ui.theme.Black50
import com.sm.myapplication.ui.theme.Gray
import com.sm.myapplication.ui.theme.GreenPrimary

private data class RankRow(val rank: Int, val name: String, val time: String)
private data class TopRanker(val rank: Int, val name: String, val barHeight: Int)

private val FILTERS = listOf("친구", "전체", "고1", "고2", "고3", "대학생")
private val DUMMY_TOP3 = listOf(
    TopRanker(2, "김철수", 73),
    TopRanker(1, "김철수", 92),
    TopRanker(3, "김영희", 66),
)
private val DUMMY_LIST = (4..10).map { RankRow(it, "이연주", "11:11:11") }

@Composable
fun RankScreen(onOpenTier: () -> Unit) {
    var pureModeOnly by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
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
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Black50)
                }
                Spacer(Modifier.weight(1f))
                Text("랭킹", color = Black50, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        // 순공 모드만 보기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { pureModeOnly = !pureModeOnly },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (pureModeOnly) GreenPrimary else Color.White)
                    .border(1.5.dp, if (pureModeOnly) GreenPrimary else Gray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (pureModeOnly) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.size(8.dp))
            Text("순공 모드만 보기", fontSize = 13.sp, color = Black50, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))

        // 필터 칩들 (가로 스크롤 대신 균등 분할)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FILTERS.forEachIndexed { idx, name ->
                FilterChip(label = name, selected = selectedFilter == idx) { selectedFilter = idx }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Top 3 podium
        Podium3(top3 = DUMMY_TOP3)

        Spacer(Modifier.height(14.dp))

        // 랭킹 리스트
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp),
        ) {
            // 헤더
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("순위", color = Black50, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(48.dp))
                Text("이름", color = Black50, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("시간", color = Black50, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
            DUMMY_LIST.forEach { row ->
                RankListRow(row)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFFD9D9D9).copy(alpha = 0.5f) else Color.Transparent)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Black50, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Podium3(top3: List<TopRanker>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        top3.forEach { t ->
            PodiumColumn(t, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumColumn(ranker: TopRanker, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 원형 프로필
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Gray),
        )
        Spacer(Modifier.height(6.dp))
        // 이름 라벨
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(BgGreenLight)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(ranker.name, fontSize = 11.sp, color = Black50, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        // 막대 (높이 다름)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ranker.barHeight.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(GreenPrimary),
            contentAlignment = Alignment.TopCenter,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(top = 6.dp).size(22.dp),
            )
        }
    }
}

@Composable
private fun RankListRow(row: RankRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${row.rank}", color = Black50, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.width(28.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Gray),
        )
        Spacer(Modifier.size(8.dp))
        Text(row.name, color = Color(0xFF1B1A1C), fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(row.time, color = Color(0xFF1B1A1C), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun TierRankDialog(onClose: () -> Unit) {
    val tiers = listOf(
        "공부 신" to "xxh 이상",
        "공부 마스터" to "xxh 이상 ~ xxh 이하",
        "공부중독" to "xxh 이상 ~ xxh 이하",
        "공부 입문" to "xxh 이상 ~ xxh 이하",
        "공부 새싹" to "xxh 이상 ~ xxh 이하",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(30.dp))
                .background(BgGreenLight)
                .padding(12.dp)
                .clickable(enabled = false) {},
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BgGreenLight)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Black50)
                }
                Spacer(Modifier.weight(1f))
                Text("티어기준표", color = Black50, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White)
                    .padding(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    tiers.forEach { (name, range) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(name, color = Black50, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(range, color = Black50, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

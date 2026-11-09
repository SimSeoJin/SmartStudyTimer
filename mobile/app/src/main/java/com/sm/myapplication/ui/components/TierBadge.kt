package com.sm.myapplication.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.sm.myapplication.R
import com.sm.myapplication.network.dto.tierDisplayName

/** 서버 티어명(tier_meta.tier_name) → 배지 이미지. 모르는 이름이거나 티어가 없으면 null. */
@DrawableRes
fun tierBadgeRes(serverName: String?): Int? = when (serverName) {
    "Diamond" -> R.drawable.ic_tier_diamond
    "Platinum" -> R.drawable.ic_tier_platinum
    "Gold" -> R.drawable.ic_tier_gold
    "Silver" -> R.drawable.ic_tier_silver
    "Bronze" -> R.drawable.ic_tier_bronze
    else -> null
}

/** 티어 배지. 티어가 없으면 아무것도 그리지 않는다. */
@Composable
fun TierBadge(serverName: String?, size: Dp, modifier: Modifier = Modifier) {
    val res = tierBadgeRes(serverName) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = "${tierDisplayName(serverName)} 티어",
        modifier = modifier.size(size),
    )
}

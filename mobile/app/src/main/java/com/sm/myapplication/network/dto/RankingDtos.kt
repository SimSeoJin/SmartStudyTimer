package com.sm.myapplication.network.dto

data class RankingResponse(
    val memberId: Long,
    val nickname: String,
    /** 최근 7일 공부 시간 합계(초) */
    val totalTime: Long,
    /** 최근 7일 하루 평균(초) */
    val recentAvgTime: Double?,
    val rankPosition: Long,
    /** 서버 tier_meta 이름 (Diamond/Platinum/Gold/Silver/Bronze) */
    val tierName: String?,
)

/** 서버 티어명을 화면 표시용 한글로. */
fun tierDisplayName(serverName: String?): String = when (serverName) {
    "Diamond" -> "다이아"
    "Platinum" -> "플래티넘"
    "Gold" -> "골드"
    "Silver" -> "실버"
    "Bronze" -> "브론즈"
    null -> "-"
    else -> serverName
}

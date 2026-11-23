package com.sm.myapplication.network.dto

data class StudyRecordRequest(
    val memberId: Long,
    val startTime: String,
    val endTime: String,
    /** StudyMode 이름(PURE/BASIC). 서버는 PURE만 랭킹·티어에 반영한다. */
    val mode: String,
)

/** 서버 기록 한 건. 시간은 "yyyy-MM-ddTHH:mm:ss"(기기 현지 시각) 형식이다. */
data class StudyRecordResponse(
    val recordId: Long,
    val startTime: String,
    val endTime: String,
    val mode: String?,
)

package com.sm.myapplication.network.dto

data class StudyRecordRequest(
    val memberId: Long,
    val startTime: String,
    val endTime: String,
    /** StudyMode 이름(PURE/BASIC). 서버는 PURE만 랭킹·티어에 반영한다. */
    val mode: String,
)

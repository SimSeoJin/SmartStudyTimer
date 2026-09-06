package com.sm.myapplication.network.dto

data class StudyRecordRequest(
    val memberId: Long,
    val startTime: String,
    val endTime: String,
    val studyMinutes: Int,
)

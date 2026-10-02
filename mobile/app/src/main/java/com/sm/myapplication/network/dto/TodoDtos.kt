package com.sm.myapplication.network.dto

// 서버 TodoRequest/TodoResponse와 맞춘 DTO. 날짜는 ISO(yyyy-MM-dd) 문자열.
// category/timeMinutes는 서버가 가지고 있지 않아 앱에만 저장한다.
data class TodoRequest(
    val title: String,
    val date: String,
    val memo: String,
    val done: Boolean,
)

data class TodoResponse(
    val todoId: Long,
    val title: String,
    val date: String,
    val memo: String?,
    val done: Boolean,
)

data class DDayRequest(
    val targetDate: String,
    val label: String,
)

data class DDayResponse(
    val targetDate: String,
    val label: String,
)

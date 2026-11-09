package com.sm.myapplication.network.dto

data class MemberInfoResponse(
    val memberId: Long,
    val nickname: String,
    val loginId: String?,
    val oauthProvider: String?,
)

data class MemberUpdateRequest(
    val nickname: String,
)

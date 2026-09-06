package com.sm.myapplication.network.dto

data class LoginRequest(
    val id: String,
    val password: String,
)

data class JoinRequest(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val password: String,
)

data class KakaoTokenRequest(
    val accessToken: String,
)

data class AuthResponse(
    val accessToken: String,
    val memberId: Long,
    val name: String,
)

data class ErrorResponse(
    val message: String?,
)

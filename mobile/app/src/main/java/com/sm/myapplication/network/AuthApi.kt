package com.sm.myapplication.network

import com.sm.myapplication.network.dto.AuthResponse
import com.sm.myapplication.network.dto.JoinRequest
import com.sm.myapplication.network.dto.KakaoTokenRequest
import com.sm.myapplication.network.dto.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("join")
    suspend fun join(@Body request: JoinRequest): Response<Unit>

    @POST("auth/kakao")
    suspend fun kakaoLogin(@Body request: KakaoTokenRequest): Response<AuthResponse>
}

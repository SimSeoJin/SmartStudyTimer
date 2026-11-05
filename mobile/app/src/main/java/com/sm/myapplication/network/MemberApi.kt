package com.sm.myapplication.network

import com.sm.myapplication.network.dto.MemberInfoResponse
import com.sm.myapplication.network.dto.MemberUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface MemberApi {
    @GET("member/{memberId}")
    suspend fun get(@Path("memberId") memberId: Long): Response<MemberInfoResponse>

    // 닉네임 변경. 이미 쓰는 닉네임이면 409.
    @PATCH("member/{memberId}")
    suspend fun update(@Path("memberId") memberId: Long, @Body request: MemberUpdateRequest): Response<MemberInfoResponse>
}

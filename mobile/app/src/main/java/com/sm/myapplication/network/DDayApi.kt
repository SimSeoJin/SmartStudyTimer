package com.sm.myapplication.network

import com.sm.myapplication.network.dto.DDayRequest
import com.sm.myapplication.network.dto.DDayResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface DDayApi {
    // 설정된 D-Day가 없으면 서버가 404를 돌려준다.
    @GET("dday/{memberId}")
    suspend fun get(@Path("memberId") memberId: Long): Response<DDayResponse>

    // 설정/수정 통합(upsert)
    @PUT("dday/{memberId}")
    suspend fun set(@Path("memberId") memberId: Long, @Body request: DDayRequest): Response<DDayResponse>
}

package com.sm.myapplication.network

import com.sm.myapplication.network.dto.RankingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface RankingApi {
    // 최근 7일 순공 시간 기준 전체 랭킹(순위 오름차순). 기록이 없는 회원은 빠져 있다.
    @GET("ranking")
    suspend fun getAll(): Response<List<RankingResponse>>

    // 내 랭킹 한 줄. 최근 7일 기록이 없으면 404.
    @GET("ranking/{memberId}")
    suspend fun getMine(@Path("memberId") memberId: Long): Response<RankingResponse>
}

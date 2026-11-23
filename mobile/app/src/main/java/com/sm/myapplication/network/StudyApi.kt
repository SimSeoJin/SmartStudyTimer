package com.sm.myapplication.network

import com.sm.myapplication.network.dto.StudyRecordRequest
import com.sm.myapplication.network.dto.StudyRecordResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StudyApi {
    // 백엔드가 이 응답을 JSON이 아닌 순수 텍스트("success")로 반환하므로 ResponseBody로 받는다.
    @POST("study/record")
    suspend fun recordStudy(@Body request: StudyRecordRequest): Response<ResponseBody>

    // 내 기록 전체(회원은 토큰으로 정해진다). 새 기기에서 로그인했을 때 내려받는다.
    @GET("study/record")
    suspend fun getRecords(): Response<List<StudyRecordResponse>>
}

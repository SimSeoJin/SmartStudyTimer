package com.sm.myapplication.network

import com.sm.myapplication.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // local.properties의 API_BASE_URL로 오버라이드 가능 (기본값은 에뮬레이터 전용 주소).
    private val BASE_URL = BuildConfig.API_BASE_URL

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val studyApi: StudyApi = retrofit.create(StudyApi::class.java)
}

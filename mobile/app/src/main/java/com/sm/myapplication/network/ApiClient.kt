package com.sm.myapplication.network

import android.util.Log
import com.sm.myapplication.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val TAG = "ApiClient"

    // local.properties의 API_BASE_URL로 오버라이드 가능 (기본값은 에뮬레이터 전용 주소).
    private val BASE_URL = BuildConfig.API_BASE_URL

    // 로그인 후 저장된 JWT를 돌려주는 함수. AppRepository 생성 시 주입한다.
    @Volatile var tokenProvider: () -> String? = { null }

    // 토큰을 붙였는데도 서버가 401로 거절했을 때 알린다(만료/서명 불일치/서버 초기화 등). 화면 쪽에서 로그인 화면으로 돌려보낸다.
    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    private val PUBLIC_PATHS = setOf("/login", "/join", "/auth/kakao")

    // 서버는 로그인/가입 외 모든 요청에 Authorization: Bearer 토큰을 요구한다.
    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        val response = chain.proceed(request)
        if (response.code == 401) {
            val path = request.url.encodedPath
            val attached = !token.isNullOrBlank()
            Log.w(TAG, "401 ${request.method} $path (토큰 ${if (attached) "첨부됨" else "없음"})")
            if (attached && path !in PUBLIC_PATHS) _unauthorized.tryEmit(Unit)
        }
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val studyApi: StudyApi = retrofit.create(StudyApi::class.java)
    val todoApi: TodoApi = retrofit.create(TodoApi::class.java)
    val ddayApi: DDayApi = retrofit.create(DDayApi::class.java)
}

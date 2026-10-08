package com.sm.myapplication

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.sm.myapplication.data.repository.AppRepository

class StudyTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        syncWhenNetworkReturns()
    }

    // 오프라인에서 저장한 Todo/D-Day가 네트워크가 돌아오는 순간 서버로 올라가도록 동기화를 다시 돌린다.
    // 등록 직후 이미 연결돼 있으면 콜백이 바로 한 번 오는데, 앱 시작 동기화(AppNavHost)와 겹치므로 건너뛴다.
    private fun syncWhenNetworkReturns() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        var initial = true
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (initial) { initial = false; return }
                    // 로그인 전이면 syncWithServer가 알아서 건너뛴다.
                    AppRepository.get(applicationContext).syncInBackground()
                }
            })
        }.onFailure { Log.w("StudyTimerApp", "네트워크 콜백 등록 실패", it) }
    }
}

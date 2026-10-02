package com.sm.myapplication.network

import android.util.Base64
import org.json.JSONObject

object JwtUtils {
    /**
     * 토큰 payload의 exp(초)를 읽어 아직 만료 전인지 확인한다. 서명 검증은 서버 몫이므로 여기선 만료만 본다.
     * 토큰이 없거나 형식이 깨졌으면 유효하지 않은 것으로 본다.
     */
    fun isNotExpired(token: String?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (token.isNullOrBlank()) return false
        return runCatching {
            val payload = token.split(".")[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            val expSec = JSONObject(json).getLong("exp")
            expSec * 1000 > nowMs
        }.getOrDefault(false)
    }
}

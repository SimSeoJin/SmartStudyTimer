package com.sm.myapplication.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sm.myapplication.data.dao.StudySessionDao
import com.sm.myapplication.data.dao.TodoDao
import com.sm.myapplication.data.datastore.AppPreferences
import com.sm.myapplication.data.db.AppDatabase
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.network.ApiClient
import com.sm.myapplication.network.dto.ErrorResponse
import com.sm.myapplication.network.dto.JoinRequest
import com.sm.myapplication.network.dto.KakaoTokenRequest
import com.sm.myapplication.network.dto.LoginRequest

class AppRepository(
    val todoDao: TodoDao,
    val studySessionDao: StudySessionDao,
    val prefs: AppPreferences,
) {
    fun observeTodosByDate(day: Long) = todoDao.observeByDate(day)
    suspend fun addTodo(todo: TodoEntity) = todoDao.insert(todo)
    suspend fun updateTodo(todo: TodoEntity) = todoDao.update(todo)
    suspend fun deleteTodo(todo: TodoEntity) = todoDao.delete(todo)
    suspend fun toggleTodo(id: Long, done: Boolean) = todoDao.setDone(id, done)

    suspend fun cleanupAbnormalSessions() = studySessionDao.deleteAbnormal()

    suspend fun saveStudySession(start: Long, end: Long, mode: StudyMode, dateEpochDay: Long) {
        studySessionDao.insert(
            StudySessionEntity(
                startTime = start,
                endTime = end,
                durationMs = end - start,
                mode = mode,
                dateEpochDay = dateEpochDay,
            )
        )
    }

    fun observeTodaySessionMs(day: Long) = studySessionDao.observeTotalMsByDate(day)
    fun observeMonthlyTotals(fromDay: Long, toDay: Long) = studySessionDao.observeMonthlyTotals(fromDay, toDay)
    fun observeSessionsByDate(day: Long) = studySessionDao.observeByDate(day)

    val ddayEpochDay = prefs.ddayEpochDay
    val ddayLabel = prefs.ddayLabel
    suspend fun setDDay(epochDay: Long, label: String) = prefs.setDDay(epochDay, label)

    val accessToken = prefs.accessToken

    suspend fun login(id: String, password: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.login(LoginRequest(id, password))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw Exception(parseErrorMessage(response.errorBody()?.string()))
        }
        prefs.saveSession(body.accessToken, body.memberId, body.name)
    }

    suspend fun loginWithKakao(kakaoAccessToken: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.kakaoLogin(KakaoTokenRequest(kakaoAccessToken))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw Exception(parseErrorMessage(response.errorBody()?.string()))
        }
        prefs.saveSession(body.accessToken, body.memberId, body.name)
    }

    suspend fun signUp(id: String, name: String, phoneNumber: String, password: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.join(JoinRequest(id, name, phoneNumber, password))
        if (!response.isSuccessful) {
            throw Exception(parseErrorMessage(response.errorBody()?.string()))
        }
    }

    suspend fun logout() = prefs.clearSession()

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "요청 처리 중 오류가 발생했습니다."
        return runCatching { Gson().fromJson(errorBody, ErrorResponse::class.java).message }
            .getOrNull() ?: "요청 처리 중 오류가 발생했습니다."
    }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun get(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = AppDatabase.get(context)
                    AppRepository(
                        todoDao = db.todoDao(),
                        studySessionDao = db.studySessionDao(),
                        prefs = AppPreferences(context.applicationContext),
                    ).also { INSTANCE = it }
                }
            }
        }
    }
}

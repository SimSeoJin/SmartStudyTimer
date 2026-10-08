package com.sm.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.sm.myapplication.data.dao.StudySessionDao
import com.sm.myapplication.data.dao.TodoDao
import com.sm.myapplication.data.datastore.AppPreferences
import com.sm.myapplication.data.db.AppDatabase
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity
import com.sm.myapplication.network.ApiClient
import com.sm.myapplication.network.dto.DDayRequest
import com.sm.myapplication.network.dto.DevLoginRequest
import com.sm.myapplication.network.dto.ErrorResponse
import com.sm.myapplication.network.dto.JoinRequest
import com.sm.myapplication.network.dto.KakaoTokenRequest
import com.sm.myapplication.network.dto.LoginRequest
import com.sm.myapplication.network.dto.StudyRecordRequest
import com.sm.myapplication.network.dto.TodoRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class AppRepository(
    val todoDao: TodoDao,
    val studySessionDao: StudySessionDao,
    val prefs: AppPreferences,
) {
    fun observeTodosByDate(day: Long) = todoDao.observeByDate(day)

    // Todo는 로컬(Room)에 먼저 저장해 화면을 갱신하고, 서버 반영은 백그라운드에서 best-effort로 뒤따른다.
    // 서버 응답을 기다리지 않으므로 오프라인이어도 저장 직후 바로 반환된다(팝업/화면이 안 멈춘다).
    // 서버에 못 보내도(오프라인/미로그인) 로컬에는 남고, serverId가 없는 항목은 다음 동기화 때 올라간다.
    suspend fun addTodo(todo: TodoEntity): Long {
        val id = todoDao.insert(todo)
        syncScope.launch { pushTodo(id) }
        return id
    }

    suspend fun updateTodo(todo: TodoEntity) {
        todoDao.update(todo)
        syncScope.launch { pushTodo(todo.id) }
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        todoDao.delete(todo)
        todo.serverId?.let { syncScope.launch { deleteTodoOnServer(it) } }
    }

    suspend fun toggleTodo(id: Long, done: Boolean) {
        todoDao.setDone(id, done)
        syncScope.launch { pushTodo(id) }
    }

    // 서버 호출은 한 번에 하나씩 — 방금 만든 Todo를 바로 토글해도 serverId가 정해지기 전에 중복 생성되지 않게 한다.
    private val todoSyncMutex = Mutex()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun pushTodo(localId: Long) {
        todoSyncMutex.withLock {
            val memberId = prefs.memberId.first() ?: return
            // 잠금 안에서 최신 상태를 다시 읽는다(앞선 요청이 serverId를 채웠을 수 있음).
            val todo = todoDao.getById(localId) ?: return
            runCatching { createOrUpdateOnServer(memberId, todo) }
                .onFailure { Log.w(TAG, "Todo 서버 반영 실패", it) }
        }
    }

    private suspend fun createOrUpdateOnServer(memberId: Long, todo: TodoEntity) {
        val request = TodoRequest(
            title = todo.title,
            date = LocalDate.ofEpochDay(todo.dateEpochDay).toString(),
            memo = todo.memo,
            done = todo.isDone,
        )
        val serverId = todo.serverId
        val response = if (serverId == null) {
            ApiClient.todoApi.create(memberId, request)
        } else {
            ApiClient.todoApi.update(memberId, serverId, request)
        }
        if (!response.isSuccessful) {
            Log.w(TAG, "Todo 서버 반영 실패: HTTP ${response.code()}")
            return
        }
        if (serverId == null) response.body()?.let { todoDao.setServerId(todo.id, it.todoId) }
    }

    private suspend fun deleteTodoOnServer(serverId: Long) {
        todoSyncMutex.withLock {
            val memberId = prefs.memberId.first() ?: return
            runCatching { ApiClient.todoApi.delete(memberId, serverId) }
                .onSuccess { if (!it.isSuccessful) Log.w(TAG, "Todo 서버 삭제 실패: HTTP ${it.code()}") }
                .onFailure { Log.w(TAG, "Todo 서버 삭제 실패", it) }
        }
    }

    /**
     * 로그인 직후/앱 시작 시 호출. 로그인 응답을 지연시키지 않도록 백그라운드에서 돈다.
     * 화면은 Room/DataStore를 보고 있으므로 동기화가 끝나면 저절로 갱신된다.
     */
    fun syncInBackground() {
        syncScope.launch { runCatching { syncWithServer() }.onFailure { Log.w(TAG, "동기화 실패", it) } }
    }

    private suspend fun syncWithServer() {
        val memberId = prefs.memberId.first() ?: return
        adoptLocalDataFor(memberId)
        // 한쪽이 실패해도 다른 쪽은 계속 진행한다.
        runCatching { syncTodos(memberId) }.onFailure { Log.w(TAG, "Todo 동기화 실패", it) }
        runCatching { syncDDay(memberId) }.onFailure { Log.w(TAG, "D-Day 동기화 실패", it) }
    }

    // 폰에 남은 Todo/D-Day가 다른 회원 것이면 비운다(안 그러면 A의 Todo가 B 계정으로 올라간다).
    // 주인이 없던 기존 데이터(이 기능 도입 전)는 현재 회원 것으로 간주한다.
    private suspend fun adoptLocalDataFor(memberId: Long) {
        val owner = prefs.dataOwnerId.first()
        if (owner == memberId) return
        if (owner != null) {
            todoSyncMutex.withLock { todoDao.deleteAll() }
            prefs.clearDDay()
        }
        prefs.setDataOwnerId(memberId)
    }

    private suspend fun syncTodos(memberId: Long) {
        todoSyncMutex.withLock {
            // 1) 서버에 없는 로컬 Todo 올리기
            todoDao.getUnsynced().forEach { todo ->
                runCatching { createOrUpdateOnServer(memberId, todo) }
                    .onFailure { Log.w(TAG, "Todo 업로드 실패", it) }
            }
            // 2) 로컬에 없는 서버 Todo 내려받기
            val response = ApiClient.todoApi.list(memberId)
            val serverTodos = response.body()
            if (!response.isSuccessful || serverTodos == null) {
                Log.w(TAG, "Todo 목록 조회 실패: HTTP ${response.code()}")
                return
            }
            val known = todoDao.getSyncedServerIds().toSet()
            serverTodos.filter { it.todoId !in known }.forEach {
                todoDao.insert(
                    TodoEntity(
                        title = it.title,
                        dateEpochDay = LocalDate.parse(it.date).toEpochDay(),
                        memo = it.memo ?: "",
                        isDone = it.done,
                        serverId = it.todoId,
                    )
                )
            }
        }
    }

    private suspend fun syncDDay(memberId: Long) {
        val localDay = prefs.ddayEpochDay.first()
        if (localDay != null) {
            pushDDay(memberId, localDay, prefs.ddayLabel.first())
            return
        }
        val response = ApiClient.ddayApi.get(memberId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            prefs.setDDay(LocalDate.parse(body.targetDate).toEpochDay(), body.label)
        }
        // 404는 서버에도 D-Day가 없다는 뜻이라 할 일이 없다.
    }

    private suspend fun pushDDay(memberId: Long, epochDay: Long, label: String) {
        runCatching {
            ApiClient.ddayApi.set(memberId, DDayRequest(LocalDate.ofEpochDay(epochDay).toString(), label))
        }.onSuccess { if (!it.isSuccessful) Log.w(TAG, "D-Day 서버 반영 실패: HTTP ${it.code()}") }
            .onFailure { Log.w(TAG, "D-Day 서버 반영 실패", it) }
    }

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
        syncStudySessionToServer(start, end)
    }

    // 로그인 안 한 상태거나 네트워크가 안 되면 조용히 실패 — 로컬 기록(Room)은 이미 저장됐으므로 사용자 경험엔 영향 없음.
    private suspend fun syncStudySessionToServer(start: Long, end: Long) {
        val memberId = prefs.memberId.first() ?: return
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        runCatching {
            ApiClient.studyApi.recordStudy(
                StudyRecordRequest(
                    memberId = memberId,
                    startTime = isoFormat.format(Date(start)),
                    endTime = isoFormat.format(Date(end)),
                )
            )
        }.onSuccess { response ->
            if (!response.isSuccessful) Log.w(TAG, "공부 기록 전송 실패: HTTP ${response.code()}")
        }.onFailure { Log.w(TAG, "공부 기록 전송 실패", it) }
    }

    fun observeTodaySessionMs(day: Long) = studySessionDao.observeTotalMsByDate(day)
    fun observeMonthlyTotals(fromDay: Long, toDay: Long) = studySessionDao.observeMonthlyTotals(fromDay, toDay)
    fun observeSessionsByDate(day: Long) = studySessionDao.observeByDate(day)

    val ddayEpochDay = prefs.ddayEpochDay
    val ddayLabel = prefs.ddayLabel
    suspend fun setDDay(epochDay: Long, label: String) {
        prefs.setDDay(epochDay, label)
        // 서버 반영은 기다리지 않는다(오프라인이어도 팝업이 바로 닫히도록).
        prefs.memberId.first()?.let { syncScope.launch { pushDDay(it, epochDay, label) } }
    }

    val accessToken = prefs.accessToken

    suspend fun login(id: String, password: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.login(LoginRequest(id, password))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw Exception(parseErrorMessage(response.errorBody()?.string()))
        }
        prefs.saveSession(body.accessToken, body.memberId, body.name)
        onLoggedIn(body.memberId)
    }

    suspend fun loginWithKakao(kakaoAccessToken: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.kakaoLogin(KakaoTokenRequest(kakaoAccessToken))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw Exception(parseErrorMessage(response.errorBody()?.string()))
        }
        prefs.saveSession(body.accessToken, body.memberId, body.name)
        onLoggedIn(body.memberId)
    }

    // 계정이 바뀌었다면 이전 계정 데이터를 홈 화면이 뜨기 전에 비우고, 서버와의 동기화는 백그라운드로 돌린다.
    private suspend fun onLoggedIn(memberId: Long) {
        adoptLocalDataFor(memberId)
        syncInBackground()
    }

    suspend fun devLogin(testUserName: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.devLogin(DevLoginRequest(testUserName))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw Exception(parseErrorMessage(response.errorBody()?.string()))
        }
        prefs.saveSession(body.accessToken, body.memberId, body.name)
        onLoggedIn(body.memberId)
    }

    suspend fun signUp(id: String, name: String, password: String): Result<Unit> = runCatching {
        val response = ApiClient.authApi.join(JoinRequest(id, name, password))
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
        private const val TAG = "AppRepository"
        @Volatile private var INSTANCE: AppRepository? = null
        fun get(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = AppDatabase.get(context)
                    AppRepository(
                        todoDao = db.todoDao(),
                        studySessionDao = db.studySessionDao(),
                        prefs = AppPreferences(context.applicationContext),
                    ).also { repo ->
                        // OkHttp 백그라운드 스레드에서 호출되므로 runBlocking으로 저장된 토큰을 읽는다.
                        ApiClient.tokenProvider = { runBlocking { repo.prefs.accessToken.first() } }
                        INSTANCE = repo
                    }
                }
            }
        }
    }
}

package com.sm.myapplication.ui.screens.timer

import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TimerPauseCause {
    NONE,
    WAITING_FACE,
    AUTO_FACE_LOST,
    MANUAL_GESTURE,
    CAMERA_OFF,
    USER
}

data class TimerState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L,
    val mode: StudyMode = StudyMode.BASIC,
    val pauseCause: TimerPauseCause = TimerPauseCause.NONE,
    val message: String = "",
)

object TimerController {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    private var startedAt: Long = 0L
    private var elapsedBeforePause: Long = 0L
    private var sessionStartedAt: Long = 0L

    /**
     * BASIC은 기존처럼 바로 시작합니다.
     * PURE는 start(mode, autoRun = false)로 시작해서 얼굴/시작 제스처가 들어와야 타이머가 움직이게 합니다.
     */
    fun start(mode: StudyMode, autoRun: Boolean = true) {
        val now = System.currentTimeMillis()
        tickJob?.cancel()

        elapsedBeforePause = 0L
        sessionStartedAt = if (autoRun) now else 0L
        startedAt = if (autoRun) now else 0L

        _state.value = TimerState(
            isRunning = autoRun,
            elapsedMs = 0L,
            mode = mode,
            pauseCause = if (autoRun) TimerPauseCause.NONE else TimerPauseCause.WAITING_FACE,
            message = if (autoRun) "타이머 시작" else "얼굴 감지 대기 중",
        )

        if (autoRun) startTicking()
    }

    /** 기본모드의 수동 버튼용. 순공모드에서는 되도록 아래 얼굴/제스처 전용 함수를 사용하세요. */
    fun pauseResume() {
        val current = _state.value
        if (current.isRunning) {
            pauseInternal(TimerPauseCause.USER, "사용자 일시중지")
        } else {
            resumeInternal("사용자 재개")
        }
    }

    fun onFaceDetected() {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return

        when {
            current.isRunning -> updateMessage("얼굴 감지 중")
            current.pauseCause == TimerPauseCause.WAITING_FACE -> resumeInternal("얼굴 감지 → 자동 시작")
            current.pauseCause == TimerPauseCause.AUTO_FACE_LOST -> resumeInternal("얼굴 다시 감지 → 자동 재개")
            current.pauseCause == TimerPauseCause.CAMERA_OFF -> updateMessage("카메라 켜짐 → 얼굴 감지 대기")
            current.pauseCause == TimerPauseCause.MANUAL_GESTURE -> updateMessage("수동 일시중지 상태: 주먹 제스처 필요")
            current.pauseCause == TimerPauseCause.USER -> updateMessage("사용자 일시중지 상태")
        }
    }

    /**
     * [absentMs]: 얼굴이 마지막으로 보인 뒤 흐른 시간. 그만큼은 공부가 아니었으므로
     * 이번 측정 구간 안에서 되돌린다(유예 시간이 순공 시간에 섞이지 않게).
     */
    fun onFaceLost(faceAwayTimeoutMs: Long? = null, absentMs: Long = 0L) {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return

        if (current.isRunning) {
            val prefix = faceAwayTimeoutMs?.let { "${it / 1000}초 이상 " }.orEmpty()
            pauseInternal(TimerPauseCause.AUTO_FACE_LOST, "${prefix}얼굴 미감지 → 자동 일시중지", rollbackMs = absentMs)
        } else if (current.pauseCause != TimerPauseCause.MANUAL_GESTURE) {
            updateMessage("얼굴 미감지 상태")
        }
    }

    fun onGestureRecognitionChanged(enabled: Boolean) {
        val current = _state.value
        if (current.mode != StudyMode.PURE || enabled) return

        if (!current.isRunning && current.pauseCause == TimerPauseCause.MANUAL_GESTURE) {
            _state.value = current.copy(
                pauseCause = TimerPauseCause.WAITING_FACE,
                message = "손 제스처 OFF → 얼굴 감지 대기 중",
            )
        }
    }

    fun onStartGesture() {
        val current = _state.value
        if (current.mode != StudyMode.PURE || current.pauseCause == TimerPauseCause.CAMERA_OFF) return
        resumeInternal("주먹 제스처 → 시작/재개")
    }

    fun onPauseGesture() {
        val current = _state.value
        if (current.mode != StudyMode.PURE || current.pauseCause == TimerPauseCause.CAMERA_OFF) return
        pauseInternal(TimerPauseCause.MANUAL_GESTURE, "손바닥 제스처 → 수동 일시중지")
    }

    fun onCameraOff() {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return
        pauseInternal(TimerPauseCause.CAMERA_OFF, "카메라 OFF → 타이머 일시중지")
    }

    fun onCameraOn() {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return
        if (!current.isRunning && current.pauseCause == TimerPauseCause.CAMERA_OFF) {
            _state.value = current.copy(
                pauseCause = TimerPauseCause.WAITING_FACE,
                message = "카메라 ON → 얼굴 감지 대기 중",
            )
        }
    }

    /** [maxElapsedMs]: 종료 확인 창을 연 시점의 시간. 창이 떠 있던 동안은 저장하지 않는다. */
    fun stop(repo: AppRepository, maxElapsedMs: Long? = null) {
        val current = _state.value
        val elapsed = currentElapsed().let { if (maxElapsedMs != null) minOf(it, maxElapsedMs) else it }
        tickJob?.cancel()

        val sane = elapsed in 1_000L..MAX_SESSION_MS
        val start = sessionStartedAt
        if (sane && start > 0L) {
            val end = start + elapsed
            val mode = current.mode
            val day = LocalDate.now().toEpochDay()
            scope.launch {
                repo.saveStudySession(start = start, end = end, mode = mode, dateEpochDay = day)
            }
        }

        sessionStartedAt = 0L
        startedAt = 0L
        elapsedBeforePause = 0L
        _state.value = TimerState()
    }

    private fun pauseInternal(cause: TimerPauseCause, message: String, rollbackMs: Long = 0L) {
        val current = _state.value
        // 되돌리기는 지금 흐르고 있는 구간까지만(이전 구간의 기록은 건드리지 않음)
        val runningSegmentMs = if (current.isRunning && startedAt > 0L) {
            (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        } else 0L
        val elapsed = currentElapsed() - rollbackMs.coerceIn(0L, runningSegmentMs)

        if (current.isRunning) {
            elapsedBeforePause = elapsed
            tickJob?.cancel()
        }

        startedAt = 0L
        _state.value = current.copy(
            isRunning = false,
            elapsedMs = elapsed,
            pauseCause = cause,
            message = message,
        )
    }

    private fun resumeInternal(message: String) {
        val current = _state.value
        if (current.isRunning) {
            updateMessage(message)
            return
        }

        val now = System.currentTimeMillis()
        if (sessionStartedAt == 0L) {
            sessionStartedAt = now - elapsedBeforePause
        }
        startedAt = now

        _state.value = current.copy(
            isRunning = true,
            pauseCause = TimerPauseCause.NONE,
            message = message,
        )
        startTicking()
    }

    private fun updateMessage(message: String) {
        _state.value = _state.value.copy(message = message)
    }

    private fun currentElapsed(): Long {
        val s = _state.value
        return if (s.isRunning && startedAt > 0L) {
            elapsedBeforePause + (System.currentTimeMillis() - startedAt)
        } else {
            elapsedBeforePause
        }
    }

    private val MAX_SESSION_MS = 12L * 60L * 60L * 1000L

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                _state.value = _state.value.copy(elapsedMs = currentElapsed())
                delay(200)
            }
        }
    }
}

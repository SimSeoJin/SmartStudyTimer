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
            current.pauseCause == TimerPauseCause.MANUAL_GESTURE -> updateMessage("수동 일시중지 상태: 손바닥 제스처 필요")
            current.pauseCause == TimerPauseCause.USER -> updateMessage("사용자 일시중지 상태")
        }
    }

    fun onFaceLost() {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return

        if (current.isRunning) {
            pauseInternal(TimerPauseCause.AUTO_FACE_LOST, "5초 이상 얼굴 미감지 → 자동 일시중지")
        } else if (current.pauseCause != TimerPauseCause.MANUAL_GESTURE) {
            updateMessage("얼굴 미감지 상태")
        }
    }

    fun onStartGesture() {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return
        resumeInternal("손바닥 제스처 → 시작/재개")
    }

    fun onPauseGesture() {
        val current = _state.value
        if (current.mode != StudyMode.PURE) return
        pauseInternal(TimerPauseCause.MANUAL_GESTURE, "주먹 제스처 → 수동 일시중지")
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

    fun stop(repo: AppRepository) {
        val current = _state.value
        val elapsed = currentElapsed()
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

    private fun pauseInternal(cause: TimerPauseCause, message: String) {
        val current = _state.value
        val elapsed = currentElapsed()

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

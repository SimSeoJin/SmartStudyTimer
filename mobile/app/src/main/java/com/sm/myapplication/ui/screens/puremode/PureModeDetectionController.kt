package com.sm.myapplication.ui.screens.puremode

import android.os.SystemClock
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import com.sm.myapplication.vision.DetectionResult
import com.sm.myapplication.vision.PureModeUserSettings

/** 순공모드의 카메라 결과를 타이머 상태로 변환한다. UI와 서비스가 같은 규칙을 사용한다. */
class PureModeDetectionController(
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private var faceMissingStartedAt: Long? = null
    private var lastFaceObservationAt: Long? = null
    private var lastFaceResultReceivedAt: Long? = null
    private var faceAwayTimeoutMs = PureModeUserSettings.FACE_AWAY_TIMEOUT_5S
    private var gestureRecognitionEnabled = true
    private var lastGestureObservationAt: Long? = null
    private var lastCommandObservationAt: Long? = null
    private var lastCommandReceivedAt: Long? = null
    // 마지막 얼굴 감지가 '고개 숙인 자세' 보조 검출이었는지. 보조 검출은 드문드문
    // 성공하므로, 그 사이 공백 때문에 필기 중 타이머가 멈췄다 켜졌다 하지 않게 한다.
    private var lastPositiveWasStudyPose = false
    // 마지막으로 정면(기본) 검출이 얼굴을 잡은 시각. 보조 검출로 이어주는 건 여기서 일정 시간까지만.
    private var lastPrimaryPositiveAt: Long? = null

    fun reset() {
        lastPositiveWasStudyPose = false
        lastPrimaryPositiveAt = null
        faceMissingStartedAt = null
        lastFaceObservationAt = null
        lastFaceResultReceivedAt = null
        lastGestureObservationAt = null
        lastCommandObservationAt = null
        lastCommandReceivedAt = null
    }

    fun setPureModeSettings(settings: PureModeUserSettings.Snapshot) {
        faceAwayTimeoutMs = settings.faceAwayTimeoutMs
        gestureRecognitionEnabled = settings.gestureRecognitionEnabled
    }

    fun handle(result: DetectionResult) {
        val now = nowMs()
        val observed = if (result.observationElapsedMs >= 0L) result.observationElapsedMs.coerceAtMost(now) else now

        // Restore the working reference's command-first behavior. A confirmed
        // pause/resume is applied once, without a same-callback face decision
        // undoing it. In particular, a fist is not queued waiting for a face.
        val freshGesture = now - observed < gestureMaxAgeMs()
        val hasGesture = result.pauseGestureDetected || result.startGestureDetected
        val timestampedGesture = result.observationElapsedMs >= 0L
        val newerGesture = !timestampedGesture || (lastGestureObservationAt?.let { observed > it } ?: true)
        if (gestureRecognitionEnabled && freshGesture && hasGesture && newerGesture &&
            TimerController.state.value.pauseCause != TimerPauseCause.CAMERA_OFF) {
            lastGestureObservationAt = observed
            lastCommandObservationAt = observed
            lastCommandReceivedAt = now
            faceMissingStartedAt = null
            if (result.pauseGestureDetected) {
                TimerController.onPauseGesture() // Pause wins if both flags are set.
            } else {
                TimerController.onStartGesture()
            }
            return
        }

        if (result.faceAnalyzed) {
            // A pre-command frame cannot shorten the newly granted grace.
            // Face and hand workers finish independently, so compare capture
            // times here, not which callback happens to arrive first.
            if (now - observed < faceAwayTimeoutMs &&
                (lastCommandObservationAt?.let { observed >= it } ?: true) &&
                (lastFaceObservationAt == null || observed >= requireNotNull(lastFaceObservationAt))) {
                lastFaceObservationAt = observed
                // Observation time orders/rejects stale frames; reception time
                // measures grace and stream health. Model latency is not absence.
                lastFaceResultReceivedAt = now
                if (result.faceDetected && result.studyPoseRescue && !canStudyPoseSustain(now)) {
                    // 보조 검출만으로는 멈춘 타이머를 다시 켜지 않는다. 빈 자리에서
                    // 배경을 얼굴로 잘못 잡아 재개되는 일을 막는다(정면 얼굴이 보여야 재개).
                    if (faceMissingStartedAt == null) faceMissingStartedAt = now
                } else if (result.faceDetected) {
                    faceMissingStartedAt = null
                    lastPositiveWasStudyPose = result.studyPoseRescue
                    if (!result.studyPoseRescue) lastPrimaryPositiveAt = now
                    TimerController.onFaceDetected()
                } else if (faceMissingStartedAt == null) {
                    faceMissingStartedAt = now
                }
            }
        }
        checkTimeout()
    }

    /** Called independently of model callbacks: stalled analysis also fails closed. */
    fun checkTimeout() {
        val now = nowMs()
        if (lastFaceResultReceivedAt == null && lastCommandReceivedAt == null && faceMissingStartedAt == null) {
            faceMissingStartedAt = now
        }
        if (faceTimeoutExpired(now)) {
            // 얼굴이 없던 구간(유예 시간 포함)은 순공 시간에서 뺀다.
            val absentSince = faceMissingStartedAt
                ?: listOfNotNull(lastFaceResultReceivedAt, lastCommandReceivedAt).maxOrNull()
            val absentMs = absentSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L
            TimerController.onFaceLost(absenceGraceMs(now), absentMs)
        }
    }

    /**
     * 보조 검출(고개 숙인 자세)로 측정을 이어줄 수 있는지: 지금 측정 중이고,
     * 정면 얼굴을 본 지 [STUDY_POSE_MAX_MS] 이내일 때만.
     */
    private fun canStudyPoseSustain(now: Long): Boolean =
        TimerController.state.value.isRunning &&
            lastPrimaryPositiveAt?.let { now - it <= STUDY_POSE_MAX_MS } == true

    /** 고개 숙인 자세로만 잡히는 중이면 사용자 설정보다 길게 기다린다. */
    private fun absenceGraceMs(now: Long): Long =
        if (lastPositiveWasStudyPose && canStudyPoseSustain(now)) {
            maxOf(faceAwayTimeoutMs, STUDY_POSE_GRACE_MS)
        } else faceAwayTimeoutMs

    private fun gestureMaxAgeMs(): Long = minOf(faceAwayTimeoutMs, 3_000L)

    private fun faceTimeoutExpired(now: Long): Boolean {
        val missingExpired = faceMissingStartedAt?.let { now - it >= absenceGraceMs(now) } ?: false
        // A real command grants a new watchdog grace, but is deliberately not
        // recorded as a positive face or as an analyzed face heartbeat.
        val streamAnchor = listOfNotNull(lastFaceResultReceivedAt, lastCommandReceivedAt).maxOrNull()
        val streamExpired = streamAnchor?.let { now - it >= faceAwayTimeoutMs } ?: false
        return missingExpired || streamExpired
    }

    companion object {
        /** 필기 자세에서 보조 검출 사이 공백을 메우는 시간. 실측 공백은 최대 약 17초였다. */
        const val STUDY_POSE_GRACE_MS = 20_000L
        /** 정면 얼굴 없이 보조 검출만으로 측정을 이어주는 최대 시간. */
        const val STUDY_POSE_MAX_MS = 5L * 60L * 1000L
    }
}

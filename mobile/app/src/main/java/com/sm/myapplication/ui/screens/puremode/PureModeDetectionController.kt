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

    fun reset() {
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
                if (result.faceDetected) {
                    faceMissingStartedAt = null
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
            TimerController.onFaceLost(faceAwayTimeoutMs)
        }
    }

    private fun gestureMaxAgeMs(): Long = minOf(faceAwayTimeoutMs, 3_000L)

    private fun faceTimeoutExpired(now: Long): Boolean {
        val missingExpired = faceMissingStartedAt?.let { now - it >= faceAwayTimeoutMs } ?: false
        // A real command grants a new watchdog grace, but is deliberately not
        // recorded as a positive face or as an analyzed face heartbeat.
        val streamAnchor = listOfNotNull(lastFaceResultReceivedAt, lastCommandReceivedAt).maxOrNull()
        val streamExpired = streamAnchor?.let { now - it >= faceAwayTimeoutMs } ?: false
        return missingExpired || streamExpired
    }
}

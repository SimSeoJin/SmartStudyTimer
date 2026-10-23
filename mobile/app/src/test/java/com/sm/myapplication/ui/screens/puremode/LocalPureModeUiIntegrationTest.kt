package com.sm.myapplication.ui.screens.puremode

import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import com.sm.myapplication.ui.screens.timer.formatElapsedDigits
import com.sm.myapplication.vision.DetectionResult
import com.sm.myapplication.vision.GestureEventFilter
import com.sm.myapplication.vision.PureModeUserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** 감지→타이머 전환과 UI에 전달되는 중간 상태까지 검사한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalPureModeUiIntegrationTest {
    private lateinit var detector: PureModeDetectionController
    private var nowMs = 0L

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        TimerController.start(StudyMode.PURE, autoRun = false)
        nowMs = 0L
        detector = PureModeDetectionController { nowMs }
    }

    @After
    fun cleanup() {
        TimerController.start(StudyMode.BASIC, autoRun = false)
        Dispatchers.resetMain()
    }

    private fun result(face: Boolean = false, start: Boolean = false, pause: Boolean = false) =
        DetectionResult(face, start, pause, emptyList(), 640, 480, true, false)

    private fun confirmFaceRecovery() {
        detector.handle(result(face = true))
        assertTrue("Keep the working baseline's immediate face recovery", TimerController.state.value.isRunning)
    }

    @Test
    fun referenceConfirmedPalmAndFistCommandsReachTimerWithSharedCooldown() {
        val filter = GestureEventFilter()
        fun sample(label: String, at: Long) {
            nowMs = at
            val event = filter.update(label, .55f, at)
            if (event.isNotEmpty()) detector.handle(gestureOnly(
                start = GestureEventFilter.isResumeCommand(event), pause = GestureEventFilter.isPauseCommand(event), captured = at))
        }
        detector.handle(result(face = true))
        sample("Open_Palm", 1_500); sample("Open_Palm", 1_650)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        detector.handle(result(face = true))
        sample("Open_Palm", 1_800)
        assertFalse(TimerController.state.value.isRunning)
        sample("Closed_Fist", 3_150); sample("Closed_Fist", 3_300)
        assertTrue(TimerController.state.value.isRunning)
        sample("Closed_Fist", 3_450)
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun clearPalmAndFistNeedTwoFramesAndFaceCannotUndoManualPause() {
        val filter = GestureEventFilter()
        detector.handle(result(face = true))
        nowMs = 1_500L
        assertEquals("", filter.update("Open_Palm", .9f, nowMs))
        assertTrue("A single confident frame no longer bypasses reference confirmation", TimerController.state.value.isRunning)
        nowMs = 1_650L
        val palm = filter.update("Open_Palm", .9f, nowMs)
        assertEquals("Open_Palm", palm)
        detector.handle(gestureOnly(pause = GestureEventFilter.isPauseCommand(palm)))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        nowMs = 1_800L
        detector.handle(result(face = true))
        assertFalse("A face result must not undo a clear palm command", TimerController.state.value.isRunning)
        nowMs = 3_150L
        assertEquals("", filter.update("Closed_Fist", .9f, nowMs))
        assertFalse(TimerController.state.value.isRunning)
        nowMs = 3_300L
        val fist = filter.update("Closed_Fist", .9f, nowMs)
        assertEquals("Closed_Fist", fist)
        detector.handle(gestureOnly(start = GestureEventFilter.isResumeCommand(fist)))
        assertTrue(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.NONE, TimerController.state.value.pauseCause)
    }

    @Test
    fun delayedOlderFistCannotUndoNewerPalm() {
        detector.handle(result(face = true))
        nowMs = 400
        detector.handle(gestureOnly(pause = true, captured = 300))
        nowMs = 500
        detector.handle(gestureOnly(start = true, captured = 100))
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        assertFalse(TimerController.state.value.isRunning)
        nowMs = 600
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 700
        detector.handle(gestureOnly(pause = true, captured = 300))
        assertTrue("An older palm must not undo a newer fist", TimerController.state.value.isRunning)
    }

    @Test
    fun sameCapturedGestureCannotExecuteTwoDifferentCommands() {
        detector.handle(result(face = true))
        nowMs = 100
        detector.handle(gestureOnly(pause = true))
        nowMs = 200
        detector.handle(gestureOnly(start = true, captured = 100))
        assertFalse(TimerController.state.value.isRunning)
        detector.handle(result(face = true))
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun simultaneousFlagsPreferManualStop() {
        detector.handle(result(face = true))
        nowMs = 100
        detector.handle(gestureOnly(start = true, pause = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun pureModeWaitsForFaceAndThenStarts() {
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.WAITING_FACE, TimerController.state.value.pauseCause)
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun palmPauseSurvivesFaceUntilFistResume() {
        detector.handle(result(face = true))
        detector.handle(result(face = true, pause = true))
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        detector.handle(result(face = true, start = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun cameraHandoverDoesNotOverrideManualPause() {
        detector.handle(result(face = true))
        TimerController.onCameraOn()
        assertTrue(TimerController.state.value.isRunning)
        detector.handle(result(pause = true))
        TimerController.onCameraOn()
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun disablingGestureReturnsToFaceWaiting() {
        detector.handle(result(pause = true))
        TimerController.onGestureRecognitionChanged(false)
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.WAITING_FACE, TimerController.state.value.pauseCause)
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun cameraRecoveryRequiresFaceBeforeTimerResumes() {
        detector.handle(result(face = true))
        TimerController.onCameraOff()
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        TimerController.onCameraOn()
        assertEquals(TimerPauseCause.WAITING_FACE, TimerController.state.value.pauseCause)
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun faceLossPausesAndFaceReturnResumes() {
        detector.handle(result(face = true))
        TimerController.onFaceLost(3_000L)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
        assertFalse(TimerController.state.value.isRunning)
        confirmFaceRecovery()
    }

    @Test
    fun generalModeDoesNotReactToCameraResults() {
        TimerController.start(StudyMode.BASIC)
        detector.handle(result(pause = true))
        TimerController.onCameraOff()
        TimerController.onFaceLost()
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun elapsedTextFormatsZeroMinuteAndHourBoundaries() {
        assertEquals("00:00:00", formatElapsedDigits(0L))
        assertEquals("00:01:00", formatElapsedDigits(60_000L))
        assertEquals("01:01:01", formatElapsedDigits(3_661_000L))
    }

    @Test
    fun cameraErrorCannotBeOverriddenByGestures() {
        TimerController.onCameraOff()
        detector.handle(result(start = true))
        detector.handle(result(pause = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.CAMERA_OFF, TimerController.state.value.pauseCause)
        TimerController.onCameraOn()
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun actualFaceMissingIntervalUsesZeroOriginAndBoundary() {
        detector.handle(result(face = true))
        detector.handle(result())
        nowMs = 4_999L
        detector.handle(result())
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 5_000L
        detector.handle(result())
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
        assertFalse(TimerController.state.value.isRunning)
        confirmFaceRecovery()
    }

    @Test
    fun fourSecondFaceAbsenceNeverEmitsEvenTransientPause() {
        detector.handle(result(face = true))
        val states = mutableListOf<com.sm.myapplication.ui.screens.timer.TimerState>()
        val observer = CoroutineScope(UnconfinedTestDispatcher()).launch {
            TimerController.state.collect { states.add(it) }
        }
        try {
            nowMs = 1_000L
            detector.handle(result())
            repeat(15) {
                nowMs += 250L
                detector.handle(DetectionResult(false, false, false, emptyList(), 640, 480, true, false, true, nowMs))
                detector.checkTimeout()
                assertTrue("Still within the 5-second grace", TimerController.state.value.isRunning)
            }
            nowMs = 5_000L // Exactly 4 seconds after the first negative.
            detector.handle(result(face = true))
            nowMs = 6_000L
            detector.checkTimeout()
            assertTrue(states.all { it.isRunning && it.pauseCause == TimerPauseCause.NONE })
            assertTrue(TimerController.state.value.isRunning)
        } finally { observer.cancel() }
    }

    @Test
    fun briefMissingFaceKeepsRunningAndReturnRestartsLossWindow() {
        detector.handle(result(face = true))
        nowMs = 1_000L
        detector.handle(result())
        nowMs = 1_500L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 2_000L
        detector.handle(result(face = true))
        nowMs = 3_000L
        detector.handle(result())
        nowMs = 7_999L
        detector.handle(result())
        assertTrue("Earlier short absence must not be added to the new window", TimerController.state.value.isRunning)
        nowMs = 8_000L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
    }

    @Test
    fun delayedNegativeStartsFullGraceWhenTheResultIsReceived() {
        detector.handle(result(face = true))
        nowMs = 4_900L
        detector.handle(DetectionResult(false, false, false, emptyList(), 640, 480, true, false, true, 1_000L))
        nowMs = 6_000L
        detector.checkTimeout()
        assertTrue("A new negative result must not inherit an almost-expired stream clock", TimerController.state.value.isRunning)
        nowMs = 9_899L
        detector.handle(result())
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 9_900L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
    }

    @Test
    fun slowFaceResultDoesNotStopAnOtherwiseHealthyStream() {
        detector.handle(result(face = true))
        nowMs = 4_500L
        detector.handle(DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, 1_000L))
        nowMs = 6_000L
        detector.checkTimeout()
        assertTrue("The stream clock must use result receipt, not analysis start", TimerController.state.value.isRunning)
    }

    private fun gestureOnly(start: Boolean = false, pause: Boolean = false, captured: Long = nowMs) =
        DetectionResult(false, start, pause, emptyList(), 640, 480, true, false, false, captured)

    @Test
    fun delayedNegativeRescueDoesNotRestartFirstAbsenceGrace() {
        detector.handle(result(face = true))
        nowMs = 500L
        detector.handle(DetectionResult(false, false, false, emptyList(), 640, 480, true, false, true, 500L))
        nowMs = 4_000L
        detector.handle(DetectionResult(false, false, false, emptyList(), 640, 480, true, false, true, 500L))
        nowMs = 5_499L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 5_500L
        detector.checkTimeout()
        assertFalse("Grace starts at the first FAST miss, not the delayed rescue", TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
    }

    @Test
    fun sameCapturedPositiveRescueCanClearAnInitialFastMiss() {
        detector.handle(result(face = true))
        nowMs = 500L
        detector.handle(DetectionResult(false, false, false, emptyList(), 640, 480, true, false, true, 500L))
        nowMs = 4_000L
        detector.handle(DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, 500L))
        nowMs = 5_500L
        detector.checkTimeout()
        assertTrue("A fresh, same-frame real positive supersedes its FAST miss", TimerController.state.value.isRunning)
    }

    @Test
    fun fistMayStartWithinInitialGraceAsInWorkingBaseline() {
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun newFistAfterFaceLossResumesImmediatelyAndRestartsFullGrace() {
        detector.handle(result(face = true))
        nowMs = 1_000L
        detector.handle(result())
        nowMs = 6_000L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        nowMs = 7_000L
        detector.handle(gestureOnly(start = true))
        assertTrue("The reference applies a real resume command without waiting for a face", TimerController.state.value.isRunning)
        detector.checkTimeout()
        assertTrue("The previous stream timeout cannot immediately undo the command", TimerController.state.value.isRunning)
        nowMs = 11_999L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 12_000L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
    }

    @Test
    fun automaticFaceReturnStillResumesOnTheFirstFreshObservation() {
        detector.handle(result(face = true))
        nowMs = 5_000L
        detector.checkTimeout()
        nowMs = 5_500L
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun watchdogDoesNotPauseAnImmediatelyRecoveredFace() {
        detector.handle(result(face = true))
        nowMs = 500L
        detector.handle(result())
        nowMs = 5_500L
        detector.checkTimeout()
        nowMs = 6_000L
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 6_250L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 6_500L
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun duplicateFaceFramesDoNotChangeRecoveredRunningState() {
        detector.handle(result(face = true))
        nowMs = 5_000L
        detector.checkTimeout()
        nowMs = 5_500L
        val frame = DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, nowMs)
        detector.handle(frame)
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 6_000L
        detector.handle(frame)
        assertTrue(TimerController.state.value.isRunning)
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun faceStillResumesImmediatelyAfterRepeatedLosses() {
        detector.handle(result(face = true))
        nowMs = 5_000L
        detector.checkTimeout()
        nowMs = 5_500L
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 10_500L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        nowMs = 11_000L
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun delayedPositiveFaceCannotBrieflyResumeBeforeStreamTimeout() {
        detector.handle(result(face = true))
        nowMs = 5_000L
        detector.checkTimeout()
        nowMs = 8_000L
        detector.handle(DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, 2_500L))
        nowMs = 8_500L
        detector.handle(DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, 3_000L))
        assertFalse(TimerController.state.value.isRunning)
    }

    @Test
    fun manualFistWithinGraceKeepsOriginalResumeBehavior() {
        detector.handle(result(face = true))
        detector.handle(gestureOnly(pause = true))
        nowMs = 500L
        detector.handle(result())
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs++ // A different gesture must come from a new captured frame.
        detector.handle(gestureOnly(pause = true))
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        assertFalse(TimerController.state.value.isRunning)
        nowMs = 1_000L
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    @Test
    fun allUserTimeoutOptionsKeepTheirBoundaryAndImmediateRecovery() {
        val constructor = PureModeUserSettings.Snapshot::class.java.getDeclaredConstructor(
            java.lang.Long.TYPE, java.lang.Boolean.TYPE, String::class.java,
        ).apply { isAccessible = true }
        for (timeout in listOf(3_000L, 5_000L, 10_000L)) {
            TimerController.start(StudyMode.PURE, autoRun = false)
            detector.reset()
            detector.setPureModeSettings(constructor.newInstance(timeout, true, PureModeUserSettings.MODE_ACCURACY))
            nowMs = 0L
            detector.handle(result(face = true))
            nowMs = 1_000L
            detector.handle(result())
            nowMs = 1_000L + timeout - 1L
            detector.handle(result())
            assertTrue("Must preserve the full selected grace: $timeout", TimerController.state.value.isRunning)
            nowMs++
            detector.checkTimeout()
            assertFalse(TimerController.state.value.isRunning)
            confirmFaceRecovery()
        }
    }

    @Test
    fun watchdogPausesEvenWhenNoMoreFramesArrive() {
        detector.handle(result(face = true))
        nowMs = 4_999L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 5_000L
        detector.checkTimeout()
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
        confirmFaceRecovery()
    }

    @Test
    fun singleNegativeObservationEventuallyPausesWithoutAnotherCallback() {
        detector.handle(result(face = true))
        nowMs = 1_000L
        detector.handle(result())
        nowMs = 4_000L
        detector.handle(gestureOnly()) // No face was analyzed; must not reset loss.
        nowMs = 6_000L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
    }

    @Test
    fun gestureOnlyCallbacksCannotReplayOrRefreshPositiveFace() {
        detector.handle(result(face = true))
        nowMs = 2_000L
        detector.handle(gestureOnly())
        nowMs = 5_000L
        detector.handle(gestureOnly())
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
    }

    @Test
    fun oldFaceAndGestureResultsCannotResumeCurrentTimer() {
        detector.handle(result(face = true))
        nowMs = 5_001L
        detector.checkTimeout()
        detector.handle(DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, 0L))
        detector.handle(gestureOnly(start = true, captured = 0L))
        assertFalse(TimerController.state.value.isRunning)
        confirmFaceRecovery()
    }

    @Test
    fun watchdogNeverResumesOrOverwritesPalmPause() {
        detector.handle(result(face = true))
        detector.handle(gestureOnly(pause = true))
        nowMs = 6_000L
        detector.checkTimeout()
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
    }

    private fun manuallyPauseThenExpireFace() {
        detector.handle(result(face = true))
        nowMs = 100L
        detector.handle(gestureOnly(pause = true))
        nowMs = 1_000L
        detector.handle(result())
        nowMs = 6_000L
        detector.checkTimeout()
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        assertFalse(TimerController.state.value.isRunning)
    }

    @Test
    fun confirmedFistResumesImmediatelyAndOneExtraCooldownSampleDoesNotExtendGrace() {
        manuallyPauseThenExpireFace()
        val filter = GestureEventFilter()
        nowMs = 6_100L
        assertEquals("", filter.update("Closed_Fist", .55f, nowMs))
        nowMs = 6_250L
        val event = filter.update("Closed_Fist", .55f, nowMs)
        assertEquals("Closed_Fist", event)
        detector.handle(gestureOnly(start = GestureEventFilter.isResumeCommand(event)))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 6_400L
        assertEquals("An extra sample during the shared cooldown cannot emit a command", "", filter.update("Closed_Fist", .55f, nowMs))
        detector.handle(gestureOnly())
        nowMs = 11_249L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 11_250L
        detector.checkTimeout()
        assertFalse("With no further samples, commands, or face results the watchdog still stops analysis", TimerController.state.value.isRunning)
    }

    @Test
    fun referenceHeldFistCanEmitAgainAfterConfirmationAndCooldownAndRenewGrace() {
        manuallyPauseThenExpireFace()
        val filter = GestureEventFilter()
        nowMs = 6_100L
        assertEquals("", filter.update("Closed_Fist", .55f, nowMs))
        nowMs = 6_250L
        detector.handle(gestureOnly(start = GestureEventFilter.isResumeCommand(filter.update("Closed_Fist", .55f, nowMs))))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 6_400L
        assertEquals("", filter.update("Closed_Fist", .55f, nowMs))
        nowMs = 7_750L
        val repeatedCommand = filter.update("Closed_Fist", .55f, nowMs)
        assertEquals("The reference repeats a confirmed held pose after 1500ms; it is not latched forever", "Closed_Fist", repeatedCommand)
        detector.handle(gestureOnly(start = GestureEventFilter.isResumeCommand(repeatedCommand)))
        nowMs = 11_250L
        detector.checkTimeout()
        assertTrue("A real repeated command renewed the old watchdog deadline", TimerController.state.value.isRunning)
        nowMs = 12_749L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 12_750L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
    }

    @Test
    fun resumeCommandStartsAFreshFiveSecondWatchdogGrace() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 11_099L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 11_100L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
    }

    @Test
    fun freshDelayedFistReceivesFullGraceFromCommandReceipt() {
        manuallyPauseThenExpireFace()
        nowMs = 8_000L
        detector.handle(gestureOnly(start = true, captured = 6_100L))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 12_999L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 13_000L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
    }

    @Test
    fun preCommandFaceResultsCannotShortenOrRefreshTheNewGrace() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        nowMs = 6_200L
        detector.handle(DetectionResult(false, false, false, emptyList(), 640, 480, true, false, true, 5_000L))
        assertTrue("A delayed pre-command negative is not a new absence", TimerController.state.value.isRunning)
        nowMs = 10_000L
        detector.handle(DetectionResult(true, false, false, emptyList(), 640, 480, true, false, true, 6_000L))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 11_100L
        detector.checkTimeout()
        assertFalse("A pre-command positive cannot pretend a newer face heartbeat arrived", TimerController.state.value.isRunning)
    }

    @Test
    fun duplicateAndOlderFistsCannotExtendTheManualResumeGrace() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        nowMs = 8_000L
        detector.handle(gestureOnly(start = true, captured = 6_100L))
        nowMs = 8_100L
        detector.handle(gestureOnly(start = true, captured = 6_000L))
        nowMs = 11_099L
        detector.checkTimeout()
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 11_100L
        detector.checkTimeout()
        assertFalse("Only a newer valid command could refresh the grace", TimerController.state.value.isRunning)
    }

    @Test
    fun newerPalmOverridesResumeAndOlderPalmCannotOverrideIt() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        nowMs = 6_200L
        detector.handle(gestureOnly(pause = true, captured = 6_000L))
        nowMs = 6_300L
        detector.handle(result(face = true))
        assertTrue("An older rejected palm cannot cancel a newer fist", TimerController.state.value.isRunning)

        nowMs = 6_400L
        detector.handle(gestureOnly(pause = true))
        nowMs = 11_400L
        detector.checkTimeout()
        nowMs = 11_500L
        detector.handle(gestureOnly(start = true))
        nowMs = 11_600L
        detector.handle(gestureOnly(pause = true))
        nowMs = 11_700L
        detector.handle(result(face = true))
        assertFalse("A newer palm explicitly stops the resumed timer", TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun combinedFaceAndBothCommandsPreferPalmWithoutAnyTransientResume() {
        manuallyPauseThenExpireFace()
        val runningStates = mutableListOf<Boolean>()
        val observer = CoroutineScope(UnconfinedTestDispatcher()).launch {
            TimerController.state.collect { runningStates.add(it.isRunning) }
        }
        try {
            nowMs = 6_200L
            detector.handle(DetectionResult(true, true, true, emptyList(), 640, 480, true, false, true, nowMs))
            assertTrue("The simultaneous palm wins without even a transient resume", runningStates.none { it })
            assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
            nowMs = 6_300L
            detector.handle(result(face = true))
            assertFalse(TimerController.state.value.isRunning)
        } finally { observer.cancel() }
    }

    @Test
    fun aFistWhileGesturesAreDisabledCannotBeReplayedAfterReenabling() {
        manuallyPauseThenExpireFace()
        val constructor = PureModeUserSettings.Snapshot::class.java.getDeclaredConstructor(
            java.lang.Long.TYPE, java.lang.Boolean.TYPE, String::class.java,
        ).apply { isAccessible = true }
        detector.setPureModeSettings(constructor.newInstance(5_000L, false, PureModeUserSettings.MODE_ACCURACY))
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        assertFalse(TimerController.state.value.isRunning)
        detector.setPureModeSettings(constructor.newInstance(5_000L, true, PureModeUserSettings.MODE_ACCURACY))
        nowMs = 6_200L
        detector.handle(result(face = true))
        assertFalse("Reenabling cannot replay the ignored command", TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun resetDoesNotReplayAnOlderFistOrReleaseManualPause() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        nowMs = 6_150L
        detector.handle(gestureOnly(pause = true))
        detector.reset()
        nowMs = 6_200L
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun cameraOffPreventsAResumeCommandFromAuthorizingANewManualPause() {
        manuallyPauseThenExpireFace()
        TimerController.onCameraOff()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        detector.checkTimeout()
        TimerController.onCameraOn()
        TimerController.onPauseGesture()
        nowMs = 6_200L
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun resumedAutomaticPauseCannotUndoALaterPalmCommand() {
        detector.handle(result(face = true))
        nowMs = 5_000L
        detector.checkTimeout()
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
        nowMs = 5_100L
        detector.handle(gestureOnly(start = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 5_200L
        detector.handle(gestureOnly(pause = true))
        nowMs = 5_300L
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun successfullyAppliedFistCannotResumeANewManualPause() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        nowMs = 6_200L
        detector.handle(result(face = true))
        assertTrue(TimerController.state.value.isRunning)
        nowMs = 6_300L
        detector.handle(gestureOnly(pause = true))
        nowMs = 6_400L
        detector.handle(result(face = true))
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
    }

    @Test
    fun firstPostCommandNegativeStartsItsOwnFullAbsenceWindow() {
        manuallyPauseThenExpireFace()
        nowMs = 6_100L
        detector.handle(gestureOnly(start = true))
        nowMs = 7_000L
        detector.handle(result())
        nowMs = 11_999L
        detector.handle(result())
        detector.checkTimeout()
        assertTrue("The full grace starts at the first new negative, not before the command", TimerController.state.value.isRunning)
        nowMs = 12_000L
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)
    }

    @Test
    fun manualResumePreservesEveryUserSelectedTimeoutAndBothRecognitionModes() {
        val constructor = PureModeUserSettings.Snapshot::class.java.getDeclaredConstructor(
            java.lang.Long.TYPE, java.lang.Boolean.TYPE, String::class.java,
        ).apply { isAccessible = true }
        for (timeout in listOf(3_000L, 5_000L, 10_000L)) {
            for (mode in listOf(PureModeUserSettings.MODE_ACCURACY, PureModeUserSettings.MODE_BATTERY_SAVER)) {
                TimerController.start(StudyMode.PURE, autoRun = false)
                detector.reset()
                detector.setPureModeSettings(constructor.newInstance(timeout, true, mode))
                nowMs = 0L
                detector.handle(result(face = true))
                nowMs = timeout
                detector.checkTimeout()
                assertFalse(TimerController.state.value.isRunning)
                nowMs++
                val commandAt = nowMs
                detector.handle(gestureOnly(start = true))
                detector.checkTimeout()
                assertTrue("A fresh command grants a new selected grace: $timeout/$mode", TimerController.state.value.isRunning)
                nowMs = commandAt + timeout - 1L
                detector.checkTimeout()
                assertTrue(TimerController.state.value.isRunning)
                nowMs++
                detector.checkTimeout()
                assertFalse("Resume cannot bypass the selected watchdog timeout", TimerController.state.value.isRunning)
            }
        }
    }

    @Test
    fun commandAtExactlyThreeSecondsOldCannotClearManualPauseOrRenewGrace() {
        manuallyPauseThenExpireFace()
        nowMs = 9_100L
        detector.handle(gestureOnly(start = true, captured = 6_100L))
        detector.checkTimeout()
        assertFalse(TimerController.state.value.isRunning)
        assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
        nowMs = 9_200L
        detector.handle(result(face = true))
        assertFalse("Rejecting an old fist cannot leave a deferred resume intent", TimerController.state.value.isRunning)
    }
}

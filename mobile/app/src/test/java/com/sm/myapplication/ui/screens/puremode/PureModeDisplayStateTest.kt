package com.sm.myapplication.ui.screens.puremode

import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import com.sm.myapplication.ui.screens.timer.TimerState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** UI projections must reduce redraws without changing the underlying timer state. */
class PureModeDisplayStateTest {
    private val running = TimerState(
        isRunning = true,
        elapsedMs = 0L,
        mode = StudyMode.PURE,
        pauseCause = TimerPauseCause.NONE,
        message = "얼굴 감지 중",
    )

    @Test
    fun elapsedTicksDoNotInvalidateCameraStatusProjection() = runBlocking {
        val states = listOf(0L, 200L, 999L, 1_000L, 1_200L, 1_999L, 2_000L)
            .map { running.copy(elapsedMs = it) }

        val output = pureModeDisplayStates(flowOf(*states.toTypedArray())).toList()

        assertEquals(listOf(pureModeDisplayState(running)), output)
    }

    @Test
    fun elapsedSecondsEmitOnlyAtWholeSecondBoundaries() = runBlocking {
        val states = listOf(0L, 200L, 999L, 1_000L, 1_001L, 1_999L, 2_000L)
            .map { running.copy(elapsedMs = it) }

        assertEquals(
            listOf(0L, 1L, 2L),
            pureModeElapsedSeconds(flowOf(*states.toTypedArray())).toList(),
        )
    }

    @Test
    fun elapsedResetEmitsZeroAndStartsANewDisplayedSequence() = runBlocking {
        val states = listOf(0L, 800L, 1_000L, 2_400L, 0L, 200L, 1_000L)
            .map { running.copy(elapsedMs = it) }

        assertEquals(
            listOf(0L, 1L, 2L, 0L, 1L),
            pureModeElapsedSeconds(flowOf(*states.toTypedArray())).toList(),
        )
    }

    @Test
    fun runningFlagChangeIsNotLostEvenWithinTheSameSecond() = runBlocking {
        val paused = running.copy(isRunning = false, elapsedMs = 200L)

        assertEquals(
            listOf(pureModeDisplayState(running), pureModeDisplayState(paused)),
            pureModeDisplayStates(flowOf(running, paused, paused.copy(elapsedMs = 999L))).toList(),
        )
    }

    @Test
    fun pauseCauseChangeIsNotLostWhenOtherStatusFieldsAreIdentical() = runBlocking {
        val waiting = running.copy(isRunning = false, pauseCause = TimerPauseCause.WAITING_FACE)
        val faceLost = waiting.copy(pauseCause = TimerPauseCause.AUTO_FACE_LOST)
        val manuallyPaused = waiting.copy(pauseCause = TimerPauseCause.MANUAL_GESTURE)
        val cameraOff = waiting.copy(pauseCause = TimerPauseCause.CAMERA_OFF)

        assertEquals(
            listOf(waiting, faceLost, manuallyPaused, cameraOff).map(::pureModeDisplayState),
            pureModeDisplayStates(flowOf(waiting, faceLost, manuallyPaused, cameraOff)).toList(),
        )
    }

    @Test
    fun messageChangeStillReachesTheStatusUiImmediately() = runBlocking {
        val resumedByFist = running.copy(message = "주먹 제스처 → 시작/재개")

        assertEquals(
            listOf(pureModeDisplayState(running), pureModeDisplayState(resumedByFist)),
            pureModeDisplayStates(flowOf(running, resumedByFist, resumedByFist)).toList(),
        )
    }

    @Test
    fun statusChangesDoNotDuplicateUnchangedDisplayedSeconds() = runBlocking {
        val paused = running.copy(
            elapsedMs = 200L,
            isRunning = false,
            pauseCause = TimerPauseCause.MANUAL_GESTURE,
            message = "손바닥 제스처",
        )
        val resumed = running.copy(elapsedMs = 200L, message = "주먹 제스처 → 시작/재개")

        assertEquals(
            listOf(0L, 1L),
            pureModeElapsedSeconds(flowOf(running, paused, resumed, resumed.copy(elapsedMs = 1_000L))).toList(),
        )
    }

    @Test
    fun displayProjectionDoesNotExposeModeOrElapsedOnlyChanges() = runBlocking {
        val modeOnlyChange = running.copy(mode = StudyMode.BASIC, elapsedMs = 12_345L)

        assertEquals(
            listOf(pureModeDisplayState(running)),
            pureModeDisplayStates(flowOf(running, modeOnlyChange)).toList(),
        )
    }

    @Test
    fun projectingPausedSessionDoesNotModifyItsRealElapsedTimeOrMode() {
        val original = running.copy(
            isRunning = false,
            elapsedMs = 12_345L,
            pauseCause = TimerPauseCause.MANUAL_GESTURE,
            message = "수동 정지",
        )
        val unchanged = original.copy()

        val projected = pureModeDisplayState(original)

        assertEquals(PureModeDisplayState(false, TimerPauseCause.MANUAL_GESTURE, "수동 정지"), projected)
        assertEquals(unchanged, original)
        assertEquals(12_345L, original.elapsedMs)
        assertEquals(StudyMode.PURE, original.mode)
    }
}

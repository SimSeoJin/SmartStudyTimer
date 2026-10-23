package com.sm.myapplication.ui.screens.puremode

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VideocamOff
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import org.junit.Assert.*
import org.junit.Test

class PureModeStatusUiTest {
    @Test
    fun manualPauseKeepsHandIconAndDistinctLabelEvenAfterFaceMessages() {
        val status = pureModeStatusUi(false, TimerPauseCause.MANUAL_GESTURE,
            "수동 일시중지 상태: 주먹 제스처 필요", "")
        assertEquals("수동 정지", status.label)
        assertEquals(Icons.Filled.PanTool, status.icon)
        assertFalse(status.activeDot)
    }

    @Test
    fun automaticFacePauseUsesItsOwnLabelRatherThanManualGestureLabel() {
        val status = pureModeStatusUi(false, TimerPauseCause.AUTO_FACE_LOST,
            "손바닥 제스처", "")
        assertEquals("얼굴 미감지", status.label)
        assertEquals(Icons.Filled.Pause, status.icon)
    }

    @Test
    fun actualCameraFailureStillTakesPrecedence() {
        val status = pureModeStatusUi(false, TimerPauseCause.CAMERA_OFF,
            "주먹 제스처", "")
        assertEquals("카메라 오류", status.label)
        assertEquals(Icons.Filled.VideocamOff, status.icon)
    }

    @Test
    fun fistResumeAndNormalFocusRemainDistinctFromBothPauseStates() {
        val gesture = pureModeStatusUi(true, TimerPauseCause.NONE, "주먹 제스처 → 시작/재개", "")
        assertEquals("다시 시작", gesture.label)
        assertEquals(Icons.Filled.PanTool, gesture.icon)
        val focus = pureModeStatusUi(true, TimerPauseCause.NONE, "얼굴 감지 중", "")
        assertEquals("집중 중", focus.label)
        assertTrue(focus.activeDot)
    }
}

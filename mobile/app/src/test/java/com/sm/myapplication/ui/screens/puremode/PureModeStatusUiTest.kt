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
        assertEquals("동지, 자리에 없군요", status.label)
        assertEquals(Icons.Filled.Pause, status.icon)
        assertTrue(status.alert)
    }

    @Test
    fun actualCameraFailureStillTakesPrecedence() {
        val status = pureModeStatusUi(false, TimerPauseCause.CAMERA_OFF,
            "주먹 제스처", "")
        assertEquals("감시 장비 오류", status.label)
        assertEquals(Icons.Filled.VideocamOff, status.icon)
    }

    @Test
    fun fistResumeAndNormalFocusRemainDistinctFromBothPauseStates() {
        val gesture = pureModeStatusUi(true, TimerPauseCause.NONE, "주먹 제스처 → 시작/재개", "")
        assertEquals("복귀를 환영합니다", gesture.label)
        assertEquals(Icons.Filled.PanTool, gesture.icon)
        val focus = pureModeStatusUi(true, TimerPauseCause.NONE, "얼굴 감지 중", "")
        assertEquals("순공 인정 중", focus.label)
        assertTrue(focus.activeDot)
        assertFalse(focus.alert)
    }
}

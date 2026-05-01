package com.example.myapplication;

import android.util.Log;

public class TimerStateManager {

    public enum TimerState {
        RUNNING,
        AUTO_PAUSED,
        MANUAL_PAUSED
    }

    public interface OnStateChangedListener {
        void onStateChanged(TimerState newState, String reason);
    }

    private static final String TAG = "TimerStateManager";

    private TimerState currentState = TimerState.AUTO_PAUSED;
    private final DebugTimer debugTimer;
    private final OnStateChangedListener listener;

    public TimerStateManager(DebugTimer debugTimer, OnStateChangedListener listener) {
        this.debugTimer = debugTimer;
        this.listener = listener;
    }

    public TimerState getCurrentState() {
        return currentState;
    }

    public void onFaceDetected() {
        Log.d(TAG, "onFaceDetected()");
        if (currentState == TimerState.AUTO_PAUSED) {
            changeState(TimerState.RUNNING, "얼굴 다시 감지 → 자동 재개");
            debugTimer.start();
        }
    }

    public void onFaceLost() {
        Log.d(TAG, "onFaceLost()");
        if (currentState == TimerState.RUNNING) {
            changeState(TimerState.AUTO_PAUSED, "4초 연속 얼굴 미감지 → 자동 일시중지");
            debugTimer.pause();
        }
    }

    public void onStartGesture() {
        Log.d(TAG, "onStartGesture()");
        if (currentState == TimerState.MANUAL_PAUSED || currentState == TimerState.AUTO_PAUSED) {
            changeState(TimerState.RUNNING, "시작 제스처 → 실행");
            debugTimer.start();
        }
    }

    public void onPauseGesture() {
        Log.d(TAG, "onPauseGesture()");
        if (currentState == TimerState.RUNNING || currentState == TimerState.AUTO_PAUSED) {
            changeState(TimerState.MANUAL_PAUSED, "일시중지 제스처 → 수동 일시중지");
            debugTimer.pause();
        }
    }

    public void onCameraOff() {
        Log.d(TAG, "onCameraOff()");
        if (currentState == TimerState.RUNNING) {
            changeState(TimerState.AUTO_PAUSED, "카메라 OFF → 자동 일시중지");
            debugTimer.pause();
        }
    }

    private void changeState(TimerState newState, String reason) {
        currentState = newState;
        Log.d(TAG, "STATE = " + newState + " / reason = " + reason);
        if (listener != null) {
            listener.onStateChanged(newState, reason);
        }
    }
}
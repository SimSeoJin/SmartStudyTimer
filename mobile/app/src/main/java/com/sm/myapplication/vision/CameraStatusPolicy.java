package com.sm.myapplication.vision;

/** Camera connection failures are distinct from optional preview/display warnings. */
public final class CameraStatusPolicy {
    private CameraStatusPolicy() { }

    public static boolean isConnectionFailure(String message) {
        if (message == null) return false;
        return message.startsWith("카메라 시작 실패")
                || message.startsWith("카메라 연결 실패")
                || message.startsWith("카메라 서비스 연결 실패")
                || message.startsWith("카메라 오류");
    }

    public static String forDisplay(String message) {
        if (message == null || message.contains("완료")
                || message.startsWith("카메라 화면 표시 실패")) return "";
        return message;
    }

    public static boolean isFreshAnalysisHeartbeat(DetectionResult result, long now, long timeout) {
        if (!result.faceAnalyzed) return false;
        return result.observationElapsedMs < 0
                || (result.observationElapsedMs <= now && now - result.observationElapsedMs < timeout);
    }
}

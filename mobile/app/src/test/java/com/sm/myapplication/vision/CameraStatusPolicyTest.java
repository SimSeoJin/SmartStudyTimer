package com.sm.myapplication.vision;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraStatusPolicyTest {
    private DetectionResult frame(boolean analyzed, long timestamp) {
        return new DetectionResult(false, false, false, Collections.emptyList(),
                640, 480, true, false, analyzed, timestamp);
    }

    @Test public void previewFailureIsNotCameraFailure() {
        assertFalse(CameraStatusPolicy.isConnectionFailure("카메라 화면 표시 실패"));
        assertEquals("", CameraStatusPolicy.forDisplay("카메라 화면 표시 실패"));
    }

    @Test public void realConnectionFailuresRemainVisible() {
        for (String failure : new String[] { "카메라 시작 실패", "카메라 시작 실패: SecurityException",
                "카메라 연결 실패: no camera", "카메라 서비스 연결 실패", "카메라 오류" }) {
            assertTrue(failure, CameraStatusPolicy.isConnectionFailure(failure));
            assertEquals(failure, CameraStatusPolicy.forDisplay(failure));
        }
    }

    @Test public void healthyStatusAndExposureWarningAreNotConnectionFailures() {
        for (String message : new String[] { "분석 중", "전면 카메라 연결 완료", "카메라 시작 중", "노출 보정 실패" }) {
            assertFalse(message, CameraStatusPolicy.isConnectionFailure(message));
        }
        assertEquals("", CameraStatusPolicy.forDisplay("전면 카메라 연결 완료"));
        assertEquals("", CameraStatusPolicy.forDisplay(null));
        assertFalse(CameraStatusPolicy.isConnectionFailure(null));
    }

    @Test public void freshFaceAnalysisClearsStaleCameraFailureEvenWithNoFace() {
        assertTrue(CameraStatusPolicy.isFreshAnalysisHeartbeat(frame(true, 1_000L), 1_500L, 5_000L));
    }

    @Test public void delayedHandResultMustNotClearACameraFailure() {
        assertFalse(CameraStatusPolicy.isFreshAnalysisHeartbeat(frame(false, 1_000L), 1_500L, 5_000L));
    }

    @Test public void expiredOrFutureFaceResultMustNotClearACameraFailure() {
        assertFalse(CameraStatusPolicy.isFreshAnalysisHeartbeat(frame(true, 1_000L), 6_000L, 5_000L));
        assertFalse(CameraStatusPolicy.isFreshAnalysisHeartbeat(frame(true, 7_000L), 6_000L, 5_000L));
    }
}

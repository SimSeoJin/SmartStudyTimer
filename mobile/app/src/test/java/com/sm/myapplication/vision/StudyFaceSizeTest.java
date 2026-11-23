package com.sm.myapplication.vision;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StudyFaceSizeTest {
    @Test
    public void backgroundSizedObjectFromTabletTestIsRejected() {
        // 실기기에서 빈 자리일 때 오른쪽 위에 잡힌 16x17px 상자
        assertFalse(StudyFaceSize.isStudyDistance(16, 17, 640, 480));
        assertFalse(StudyFaceSize.isStudyDistance(47, 40, 640, 480));
    }

    @Test
    public void realStudyFacesFromTabletTestAreKept() {
        // 고개 숙인 부분 얼굴(136x118, 183x183)과 경계값
        assertTrue(StudyFaceSize.isStudyDistance(136, 118, 640, 480));
        assertTrue(StudyFaceSize.isStudyDistance(183, 183, 640, 480));
        assertTrue(StudyFaceSize.isStudyDistance(48, 30, 640, 480));
        assertTrue("세로 화면도 짧은 변 기준", StudyFaceSize.isStudyDistance(48, 48, 480, 640));
    }

    @Test
    public void faceClippedAtSideOrTopMeansLeavingNotHeadDown() {
        // 자리를 뜰 때(실측): 61x123@579,183 → 오른쪽 끝, 125x214@515,21 → 위쪽 끝
        assertTrue(StudyFaceSize.touchesExitEdge(579, 183, 640, 640));
        assertTrue(StudyFaceSize.touchesExitEdge(515, 21, 640, 640));
        assertTrue(StudyFaceSize.touchesExitEdge(0, 200, 120, 640));
        // 고개 숙인 자세(실측): 136x118@31,362(아래쪽 잘림), 183x183@137,100
        assertFalse(StudyFaceSize.touchesExitEdge(31, 362, 167, 640));
        assertFalse(StudyFaceSize.touchesExitEdge(137, 100, 320, 640));
    }

    @Test
    public void invalidSizesAreRejected() {
        assertFalse(StudyFaceSize.isStudyDistance(0, 100, 640, 480));
        assertFalse(StudyFaceSize.isStudyDistance(100, 100, 0, 480));
    }
}

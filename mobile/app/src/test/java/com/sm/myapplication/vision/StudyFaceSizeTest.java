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
    public void invalidSizesAreRejected() {
        assertFalse(StudyFaceSize.isStudyDistance(0, 100, 640, 480));
        assertFalse(StudyFaceSize.isStudyDistance(100, 100, 0, 480));
    }
}

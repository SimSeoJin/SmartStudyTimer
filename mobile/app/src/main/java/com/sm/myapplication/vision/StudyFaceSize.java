package com.sm.myapplication.vision;

import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;

import java.util.ArrayList;
import java.util.List;

/**
 * 공부하는 거리(카메라 앞 책상)의 얼굴만 인정한다.
 * 실측(태블릿 640x480): 정면 얼굴은 화면 대부분, 고개 숙인 부분 얼굴도 118~194px.
 * 빈 자리에서는 배경의 16x17px짜리 물체가 얼굴로 잡혀 타이머가 계속 돌았다.
 */
public final class StudyFaceSize {
    /** 얼굴 상자의 긴 변이 화면 짧은 변의 이 비율 이상이어야 한다(480px 기준 48px). */
    static final float MIN_FRACTION_OF_SHORT_SIDE = 0.10f;

    private StudyFaceSize() { }

    public static boolean isStudyDistance(int boxWidth, int boxHeight, int frameWidth, int frameHeight) {
        if (boxWidth <= 0 || boxHeight <= 0 || frameWidth <= 0 || frameHeight <= 0) return false;
        float minSide = Math.min(frameWidth, frameHeight) * MIN_FRACTION_OF_SHORT_SIDE;
        return Math.max(boxWidth, boxHeight) >= minSide;
    }

    static List<Face> keepFaces(List<Face> faces, int frameWidth, int frameHeight) {
        List<Face> kept = new ArrayList<>();
        for (Face face : faces) {
            Rect box = face.getBoundingBox();
            if (isStudyDistance(box.width(), box.height(), frameWidth, frameHeight)) kept.add(face);
        }
        return kept;
    }

    static List<Rect> keepBoxes(List<Rect> boxes, int frameWidth, int frameHeight) {
        List<Rect> kept = new ArrayList<>();
        for (Rect box : boxes) {
            if (isStudyDistance(box.width(), box.height(), frameWidth, frameHeight)) kept.add(box);
        }
        return kept;
    }
}

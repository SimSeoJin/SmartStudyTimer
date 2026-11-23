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

    /**
     * 화면 왼쪽/오른쪽/위쪽 끝에 걸린 얼굴 = 옆으로 걸어 나가거나 일어서는 중.
     * 고개 숙인 자세(아래쪽으로 잘리거나 가운데)와 달리 오래 기다려 줄 이유가 없다.
     * 실측: 자리를 뜰 때 61x123@579,183 · 125x214@515,21(폭 640),
     * 고개 숙였을 때 136x118@31,362(아래쪽 잘림) · 183x183@137,100.
     */
    public static boolean touchesExitEdge(Rect box, int frameWidth) {
        return touchesExitEdge(box.left, box.top, box.right, frameWidth);
    }

    static boolean touchesExitEdge(int left, int top, int right, int frameWidth) {
        if (frameWidth <= 0) return false;
        float margin = frameWidth * EXIT_EDGE_FRACTION;
        return left <= margin || right >= frameWidth - margin || top <= margin;
    }

    static final float EXIT_EDGE_FRACTION = 0.04f;

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

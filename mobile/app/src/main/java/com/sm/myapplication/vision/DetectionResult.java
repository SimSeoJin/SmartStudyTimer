package com.sm.myapplication.vision;

import android.graphics.Rect;

import java.util.List;

public class DetectionResult {
    public final boolean faceDetected;
    public final boolean startGestureDetected;
    public final boolean pauseGestureDetected;
    public final List<Rect> faces;
    public final int imageWidth;
    public final int imageHeight;
    public final boolean frontCamera;
    public final boolean lowLightApplied;
    // Gesture-only callbacks must not replay a cached face observation.
    public final boolean faceAnalyzed;
    public final long observationElapsedMs;

    public DetectionResult(
            boolean faceDetected,
            boolean startGestureDetected,
            boolean pauseGestureDetected,
            List<Rect> faces,
            int imageWidth,
            int imageHeight,
            boolean frontCamera,
            boolean lowLightApplied
    ) {
        this(faceDetected, startGestureDetected, pauseGestureDetected, faces, imageWidth,
                imageHeight, frontCamera, lowLightApplied, true, -1L);
    }

    public DetectionResult(boolean faceDetected, boolean startGestureDetected,
            boolean pauseGestureDetected, List<Rect> faces, int imageWidth, int imageHeight,
            boolean frontCamera, boolean lowLightApplied, boolean faceAnalyzed,
            long observationElapsedMs) {
        this.faceDetected = faceDetected;
        this.startGestureDetected = startGestureDetected;
        this.pauseGestureDetected = pauseGestureDetected;
        this.faces = faces;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.frontCamera = frontCamera;
        this.lowLightApplied = lowLightApplied;
        this.faceAnalyzed = faceAnalyzed;
        this.observationElapsedMs = observationElapsedMs;
    }
}

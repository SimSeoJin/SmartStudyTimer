package com.example.myapplication;

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
    public final String debugText;

    public DetectionResult(
            boolean faceDetected,
            boolean startGestureDetected,
            boolean pauseGestureDetected,
            List<Rect> faces,
            int imageWidth,
            int imageHeight,
            boolean frontCamera,
            String debugText
    ) {
        this.faceDetected = faceDetected;
        this.startGestureDetected = startGestureDetected;
        this.pauseGestureDetected = pauseGestureDetected;
        this.faces = faces;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.frontCamera = frontCamera;
        this.debugText = debugText;
    }
}
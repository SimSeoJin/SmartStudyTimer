package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class HandGestureAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnGestureListener {
        void onGesture(DetectionResult result);
    }

    private static final String TAG = "HandGestureAnalyzer";
    private static final long GESTURE_ANALYZE_INTERVAL_MS = 200L;
    private static final int GESTURE_CONFIRM_THRESHOLD = 2;

    private final Context appContext;
    private final OnGestureListener listener;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private GestureRecognizerHelper gestureHelper;
    private boolean gestureInitTried = false;
    private boolean gestureAvailable = false;

    private volatile boolean simulatedStartGesture = false;
    private volatile boolean simulatedPauseGesture = false;

    private long lastAnalyzeAt = 0L;

    private String lastGestureLabel = "";
    private int sameGestureCount = 0;

    public HandGestureAnalyzer(Context context, OnGestureListener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    public void triggerStartGesture() {
        simulatedStartGesture = true;
    }

    public void triggerPauseGesture() {
        simulatedPauseGesture = true;
    }

    private void ensureGestureHelper() {
        if (gestureInitTried) return;

        gestureInitTried = true;
        gestureHelper = new GestureRecognizerHelper();
        gestureAvailable = gestureHelper.init(appContext);

        if (!gestureAvailable) {
            Log.e(TAG, "Gesture recognizer unavailable. Gesture detection disabled.");
        }
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        long now = System.currentTimeMillis();

        if (now - lastAnalyzeAt < GESTURE_ANALYZE_INTERVAL_MS) {
            imageProxy.close();
            return;
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        lastAnalyzeAt = now;

        boolean startGesture = false;
        boolean pauseGesture = false;
        String debugText = "GESTURE idle";

        try {
            boolean manualStart = simulatedStartGesture;
            boolean manualPause = simulatedPauseGesture;
            simulatedStartGesture = false;
            simulatedPauseGesture = false;

            if (manualStart) startGesture = true;
            if (manualPause) pauseGesture = true;

            if (!manualStart && !manualPause) {
                ensureGestureHelper();

                if (gestureAvailable && gestureHelper != null && gestureHelper.isReady()) {
                    Bitmap bitmap = imageProxyToBitmap(imageProxy);

                    if (bitmap != null) {
                        GestureRecognizerResult result = gestureHelper.recognize(bitmap, now);
                        String gestureName = extractTopGestureName(result);

                        if (gestureName != null) {
                            if (gestureName.equals(lastGestureLabel)) {
                                sameGestureCount++;
                            } else {
                                lastGestureLabel = gestureName;
                                sameGestureCount = 1;
                            }

                            if (sameGestureCount >= GESTURE_CONFIRM_THRESHOLD) {
                                if ("Open_Palm".equals(gestureName)) {
                                    startGesture = true;
                                } else if ("Closed_Fist".equals(gestureName)) {
                                    pauseGesture = true;
                                }

                                sameGestureCount = 0;
                                lastGestureLabel = "";
                            }
                        } else {
                            sameGestureCount = 0;
                            lastGestureLabel = "";
                        }

                        debugText = "GESTURE label=" + gestureName
                                + " start=" + startGesture
                                + " pause=" + pauseGesture
                                + " ready=" + gestureAvailable;
                    } else {
                        debugText = "GESTURE bitmap null";
                    }
                } else {
                    debugText = "GESTURE unavailable";
                }
            } else {
                debugText = "GESTURE debug-only start=" + startGesture + " pause=" + pauseGesture;
            }

            DetectionResult detectionResult = new DetectionResult(
                    false,
                    startGesture,
                    pauseGesture,
                    new ArrayList<>(),
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    true,
                    debugText
            );

            if (listener != null) {
                listener.onGesture(detectionResult);
            }

        } catch (Exception e) {
            Log.e(TAG, "Gesture analyze failed", e);

            DetectionResult detectionResult = new DetectionResult(
                    false,
                    false,
                    false,
                    new ArrayList<>(),
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    true,
                    "GESTURE error: " + e.getClass().getSimpleName() + " / " + e.getMessage()
            );

            if (listener != null) {
                listener.onGesture(detectionResult);
            }
        } finally {
            isProcessing.set(false);
            imageProxy.close();
        }
    }

    private String extractTopGestureName(GestureRecognizerResult result) {
        if (result == null || result.gestures().isEmpty() || result.gestures().get(0).isEmpty()) {
            return null;
        }

        Category top = result.gestures().get(0).get(0);
        return top.categoryName();
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            android.graphics.YuvImage yuvImage =
                    new android.graphics.YuvImage(
                            nv21,
                            android.graphics.ImageFormat.NV21,
                            imageProxy.getWidth(),
                            imageProxy.getHeight(),
                            null
                    );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                    80,
                    out
            );

            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            if (bitmap == null) return null;

            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "imageProxyToBitmap failed", e);
            return null;
        }
    }

    public void shutdown() {
        if (gestureHelper != null) {
            gestureHelper.close();
        }
    }
}
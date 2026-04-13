package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnDetectionListener {
        void onDetection(DetectionResult result);
    }

    private static final String TAG = "FrameAnalyzer";

    private static final long FACE_ANALYZE_INTERVAL_MS = 1000L;
    private static final long GESTURE_ANALYZE_INTERVAL_MS = 200L;
    private static final int GESTURE_CONFIRM_THRESHOLD = 2;

    private static final boolean ENABLE_GESTURE_RECOGNITION = true;

    private final Context appContext;
    private final OnDetectionListener listener;
    private final FaceDetector faceDetector;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private GestureRecognizerHelper gestureHelper;
    private boolean gestureInitTried = false;
    private boolean gestureAvailable = false;

    private long lastFaceAnalyzeAt = 0L;
    private long lastGestureAnalyzeAt = 0L;

    private volatile boolean simulatedStartGesture = false;
    private volatile boolean simulatedPauseGesture = false;

    private boolean lastFaceDetected = false;
    private List<Rect> lastFaceRects = new ArrayList<>();

    private String lastGestureLabel = "";
    private int sameGestureCount = 0;

    public FrameAnalyzer(Context context, OnDetectionListener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    public void triggerStartGesture() {
        simulatedStartGesture = true;
    }

    public void triggerPauseGesture() {
        simulatedPauseGesture = true;
    }

    private void ensureGestureHelper() {
        if (gestureInitTried || !ENABLE_GESTURE_RECOGNITION) return;

        gestureInitTried = true;

        try {
            gestureHelper = new GestureRecognizerHelper();
            gestureAvailable = gestureHelper.init(appContext);
        } catch (Throwable t) {
            Log.e(TAG, "Gesture helper init crashed", t);
            gestureAvailable = false;
            gestureHelper = null;
        }

        Log.d(TAG, "gestureAvailable=" + gestureAvailable);

        if (!gestureAvailable) {
            Log.e(TAG, "Gesture recognizer unavailable. Gesture disabled.");
        }
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        long now = System.currentTimeMillis();

        boolean doFace = now - lastFaceAnalyzeAt >= FACE_ANALYZE_INTERVAL_MS;
        boolean doGesture = ENABLE_GESTURE_RECOGNITION &&
                (now - lastGestureAnalyzeAt >= GESTURE_ANALYZE_INTERVAL_MS);

        if (!doFace && !doGesture) {
            imageProxy.close();
            return;
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            isProcessing.set(false);
            imageProxy.close();
            return;
        }

        final int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        final int width = imageProxy.getWidth();
        final int height = imageProxy.getHeight();

        boolean faceDetected = lastFaceDetected;
        List<Rect> faceRects = new ArrayList<>(lastFaceRects);
        boolean startGestureDetected = false;
        boolean pauseGestureDetected = false;

        String gestureLabelForDebug = "null";
        float gestureScoreForDebug = -1f;

        try {
            boolean manualStart = simulatedStartGesture;
            boolean manualPause = simulatedPauseGesture;
            simulatedStartGesture = false;
            simulatedPauseGesture = false;

            if (manualStart) startGestureDetected = true;
            if (manualPause) pauseGestureDetected = true;

            if (doGesture && !manualStart && !manualPause) {
                lastGestureAnalyzeAt = now;
                ensureGestureHelper();

                if (gestureAvailable && gestureHelper != null && gestureHelper.isReady()) {
                    Bitmap bitmap = imageProxyToBitmap(imageProxy);

                    if (bitmap != null) {
                        GestureRecognizerResult gestureResult = gestureHelper.recognize(bitmap, now);

                        if (gestureResult != null
                                && !gestureResult.gestures().isEmpty()
                                && !gestureResult.gestures().get(0).isEmpty()) {

                            Category top = gestureResult.gestures().get(0).get(0);
                            gestureLabelForDebug = top.categoryName();
                            gestureScoreForDebug = top.score();

                            Log.d(TAG, "GESTURE DETECTED label=" + gestureLabelForDebug
                                    + ", score=" + gestureScoreForDebug);

                            String gestureName = gestureLabelForDebug;

                            if (gestureName.equals(lastGestureLabel)) {
                                sameGestureCount++;
                            } else {
                                lastGestureLabel = gestureName;
                                sameGestureCount = 1;
                            }

                            if (sameGestureCount >= GESTURE_CONFIRM_THRESHOLD) {
                                if ("Open_Palm".equals(gestureName) && gestureScoreForDebug >= 0.6f) {
                                    startGestureDetected = true;
                                } else if ("Closed_Fist".equals(gestureName) && gestureScoreForDebug >= 0.6f) {
                                    pauseGestureDetected = true;
                                }

                                sameGestureCount = 0;
                                lastGestureLabel = "";
                            }
                        } else {
                            Log.d(TAG, "GESTURE DETECTED none");
                            sameGestureCount = 0;
                            lastGestureLabel = "";
                        }
                    } else {
                        gestureLabelForDebug = "bitmap_null";
                        Log.d(TAG, "GESTURE bitmap is null");
                    }
                } else {
                    gestureLabelForDebug = "gesture_unavailable";
                    Log.d(TAG, "GESTURE unavailable");
                }
            }

            if (doFace) {
                lastFaceAnalyzeAt = now;

                InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);
                List<Face> faces = com.google.android.gms.tasks.Tasks.await(faceDetector.process(image));

                faceRects.clear();
                for (Face face : faces) {
                    faceRects.add(face.getBoundingBox());
                }

                faceDetected = !faceRects.isEmpty();
                lastFaceDetected = faceDetected;
                lastFaceRects = new ArrayList<>(faceRects);
            }

            String debugText =
                    "faces=" + faceRects.size()
                            + " / gLabel=" + gestureLabelForDebug
                            + " / gScore=" + gestureScoreForDebug
                            + " / gStart=" + startGestureDetected
                            + " / gPause=" + pauseGestureDetected
                            + " / gEnabled=" + ENABLE_GESTURE_RECOGNITION
                            + " / gReady=" + gestureAvailable;

            DetectionResult result = new DetectionResult(
                    faceDetected,
                    startGestureDetected,
                    pauseGestureDetected,
                    faceRects,
                    width,
                    height,
                    true,
                    debugText
            );

            if (listener != null) {
                listener.onDetection(result);
            }

        } catch (Throwable t) {
            Log.e(TAG, "Analyze error", t);

            DetectionResult result = new DetectionResult(
                    lastFaceDetected,
                    false,
                    false,
                    new ArrayList<>(lastFaceRects),
                    width,
                    height,
                    true,
                    "analyze error: " + t.getClass().getSimpleName() + " / " + t.getMessage()
            );

            if (listener != null) {
                listener.onDetection(result);
            }
        } finally {
            isProcessing.set(false);
            imageProxy.close();
        }
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

            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        } catch (Throwable t) {
            Log.e(TAG, "imageProxyToBitmap failed", t);
            return null;
        }
    }

    public void shutdown() {
        try {
            faceDetector.close();
        } catch (Throwable t) {
            Log.e(TAG, "FaceDetector close failed", t);
        }

        try {
            if (gestureHelper != null) {
                gestureHelper.close();
            }
        } catch (Throwable t) {
            Log.e(TAG, "GestureHelper close failed", t);
        }
    }
}
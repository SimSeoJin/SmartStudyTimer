package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Tasks;
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
    private static final long GESTURE_EVENT_COOLDOWN_MS = 1500L;
    private static final int GESTURE_CONFIRM_THRESHOLD = 2;
    private static final float GESTURE_SCORE_THRESHOLD = 0.55f;

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
    private long lastGestureEventAt = 0L;

    private volatile boolean simulatedStartGesture = false;
    private volatile boolean simulatedPauseGesture = false;
    private volatile boolean frontCamera = true;

    private boolean lastFaceDetected = false;
    private List<Rect> lastFaceRects = new ArrayList<>();
    private int lastImageWidth = 0;
    private int lastImageHeight = 0;

    private String lastGestureLabel = "";
    private int sameGestureCount = 0;

    private String lastGestureLabelForDebug = "not_run";
    private float lastGestureScoreForDebug = -1f;
    private String lastGestureErrorForDebug = "none";
    private String lastGestureAssetForDebug = "not checked";

    public FrameAnalyzer(Context context, OnDetectionListener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    public void setFrontCamera(boolean frontCamera) {
        this.frontCamera = frontCamera;
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
            lastGestureErrorForDebug = gestureHelper.getLastErrorMessage();
            lastGestureAssetForDebug = gestureHelper.getLastAssetCheckMessage();
        } catch (Throwable t) {
            Log.e(TAG, "Gesture helper init crashed", t);
            gestureAvailable = false;
            gestureHelper = null;
            lastGestureErrorForDebug = t.getClass().getSimpleName() + " / " + String.valueOf(t.getMessage());
        }

        Log.d(TAG, "gestureAvailable=" + gestureAvailable
                + ", asset=" + lastGestureAssetForDebug
                + ", err=" + lastGestureErrorForDebug);
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        long now = SystemClock.uptimeMillis();

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

        boolean faceDetected = lastFaceDetected;
        List<Rect> faceRects = new ArrayList<>(lastFaceRects);
        boolean startGestureDetected = false;
        boolean pauseGestureDetected = false;
        Bitmap frameBitmap = null;

        int outputWidth = lastImageWidth > 0 ? lastImageWidth : imageProxy.getWidth();
        int outputHeight = lastImageHeight > 0 ? lastImageHeight : imageProxy.getHeight();

        try {
            boolean manualStart = simulatedStartGesture;
            boolean manualPause = simulatedPauseGesture;
            simulatedStartGesture = false;
            simulatedPauseGesture = false;

            if (manualStart) {
                startGestureDetected = true;
                lastGestureLabelForDebug = "manual_start";
                lastGestureScoreForDebug = 1.0f;
            }
            if (manualPause) {
                pauseGestureDetected = true;
                lastGestureLabelForDebug = "manual_pause";
                lastGestureScoreForDebug = 1.0f;
            }

            if (doGesture && !manualStart && !manualPause) {
                lastGestureAnalyzeAt = now;
                ensureGestureHelper();

                if (gestureHelper != null) {
                    lastGestureErrorForDebug = gestureHelper.getLastErrorMessage();
                    lastGestureAssetForDebug = gestureHelper.getLastAssetCheckMessage();
                }

                if (gestureAvailable && gestureHelper != null && gestureHelper.isReady()) {
                    frameBitmap = imageProxyToBitmap(imageProxy);

                    if (frameBitmap != null) {
                        outputWidth = frameBitmap.getWidth();
                        outputHeight = frameBitmap.getHeight();

                        GestureRecognizerResult gestureResult = gestureHelper.recognize(frameBitmap, now);

                        lastGestureErrorForDebug = gestureHelper.getLastErrorMessage();
                        lastGestureAssetForDebug = gestureHelper.getLastAssetCheckMessage();

                        if (gestureResult != null
                                && !gestureResult.gestures().isEmpty()
                                && !gestureResult.gestures().get(0).isEmpty()) {

                            Category top = gestureResult.gestures().get(0).get(0);
                            String gestureName = top.categoryName();
                            float gestureScore = top.score();

                            lastGestureLabelForDebug = gestureName;
                            lastGestureScoreForDebug = gestureScore;

                            Log.d(TAG, "GESTURE label=" + gestureName + ", score=" + gestureScore);

                            boolean usableGesture = gestureName != null
                                    && !"None".equals(gestureName)
                                    && gestureScore >= GESTURE_SCORE_THRESHOLD;

                            if (usableGesture) {
                                if (gestureName.equals(lastGestureLabel)) {
                                    sameGestureCount++;
                                } else {
                                    lastGestureLabel = gestureName;
                                    sameGestureCount = 1;
                                }

                                boolean cooldownPassed = now - lastGestureEventAt >= GESTURE_EVENT_COOLDOWN_MS;
                                if (sameGestureCount >= GESTURE_CONFIRM_THRESHOLD && cooldownPassed) {
                                    if ("Open_Palm".equals(gestureName)) {
                                        startGestureDetected = true;
                                    } else if ("Closed_Fist".equals(gestureName)) {
                                        pauseGestureDetected = true;
                                    }

                                    if (startGestureDetected || pauseGestureDetected) {
                                        lastGestureEventAt = now;
                                    }

                                    sameGestureCount = 0;
                                    lastGestureLabel = "";
                                }
                            } else {
                                sameGestureCount = 0;
                                lastGestureLabel = "";
                            }
                        } else {
                            lastGestureLabelForDebug = "no_hand";
                            lastGestureScoreForDebug = -1f;
                            sameGestureCount = 0;
                            lastGestureLabel = "";
                            Log.d(TAG, "GESTURE none");
                        }
                    } else {
                        lastGestureLabelForDebug = "bitmap_null";
                        lastGestureScoreForDebug = -1f;
                        sameGestureCount = 0;
                        lastGestureLabel = "";
                        Log.d(TAG, "GESTURE bitmap is null");
                    }
                } else {
                    lastGestureLabelForDebug = "gesture_unavailable";
                    lastGestureScoreForDebug = -1f;
                    sameGestureCount = 0;
                    lastGestureLabel = "";
                    Log.d(TAG, "GESTURE unavailable / asset=" + lastGestureAssetForDebug
                            + " / err=" + lastGestureErrorForDebug);
                }
            }

            if (doFace) {
                lastFaceAnalyzeAt = now;

                if (frameBitmap == null) {
                    frameBitmap = imageProxyToBitmap(imageProxy);
                }

                if (frameBitmap != null) {
                    outputWidth = frameBitmap.getWidth();
                    outputHeight = frameBitmap.getHeight();

                    InputImage image = InputImage.fromBitmap(frameBitmap, 0);
                    List<Face> faces = Tasks.await(faceDetector.process(image));

                    faceRects.clear();
                    for (Face face : faces) {
                        faceRects.add(face.getBoundingBox());
                    }

                    faceDetected = !faceRects.isEmpty();
                    lastFaceDetected = faceDetected;
                    lastFaceRects = new ArrayList<>(faceRects);
                    lastImageWidth = outputWidth;
                    lastImageHeight = outputHeight;
                } else {
                    lastGestureErrorForDebug = "bitmap null for face";
                }
            }

            String debugText =
                    "faces=" + faceRects.size()
                            + " / gLabel=" + lastGestureLabelForDebug
                            + " / gScore=" + lastGestureScoreForDebug
                            + " / gSame=" + sameGestureCount
                            + " / gStart=" + startGestureDetected
                            + " / gPause=" + pauseGestureDetected
                            + " / gEnabled=" + ENABLE_GESTURE_RECOGNITION
                            + " / gReady=" + (gestureHelper != null && gestureHelper.isReady())
                            + " / gInit=" + gestureAvailable
                            + " / gAsset=" + lastGestureAssetForDebug
                            + " / gErr=" + lastGestureErrorForDebug;

            DetectionResult result = new DetectionResult(
                    faceDetected,
                    startGestureDetected,
                    pauseGestureDetected,
                    faceRects,
                    outputWidth,
                    outputHeight,
                    frontCamera,
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
                    outputWidth,
                    outputHeight,
                    frontCamera,
                    "analyze error: " + t.getClass().getSimpleName() + " / " + String.valueOf(t.getMessage())
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
            Bitmap bitmap;
            if (imageProxy.getFormat() == PixelFormat.RGBA_8888 || imageProxy.getPlanes().length == 1) {
                bitmap = rgbaImageProxyToBitmap(imageProxy);
            } else {
                bitmap = yuvImageProxyToBitmap(imageProxy);
            }

            if (bitmap == null) return null;
            return rotateBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
        } catch (Throwable t) {
            Log.e(TAG, "imageProxyToBitmap failed", t);
            lastGestureErrorForDebug = "bitmap convert: " + t.getClass().getSimpleName()
                    + " / " + String.valueOf(t.getMessage());
            return null;
        }
    }

    private Bitmap rgbaImageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes == null || planes.length == 0) return null;

        ImageProxy.PlaneProxy plane = planes[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();

        if (pixelStride == 4 && rowStride == width * 4) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            return bitmap;
        }

        // 일부 기기에서는 각 row 끝에 padding이 붙을 수 있다.
        // padding까지 포함한 임시 Bitmap을 만든 뒤 실제 width만 잘라낸다.
        int bitmapWidth = rowStride / Math.max(pixelStride, 1);
        Bitmap paddedBitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        paddedBitmap.copyPixelsFromBuffer(buffer);
        return Bitmap.createBitmap(paddedBitmap, 0, 0, width, height);
    }

    private Bitmap yuvImageProxyToBitmap(ImageProxy imageProxy) {
        byte[] nv21 = yuv420888ToNv21(imageProxy);
        if (nv21 == null) return null;

        android.graphics.YuvImage yuvImage =
                new android.graphics.YuvImage(
                        nv21,
                        ImageFormat.NV21,
                        imageProxy.getWidth(),
                        imageProxy.getHeight(),
                        null
                );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean compressed = yuvImage.compressToJpeg(
                new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                90,
                out
        );
        if (!compressed) return null;

        byte[] imageBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private byte[] yuv420888ToNv21(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes == null || planes.length < 3) return null;

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        byte[] nv21 = new byte[width * height * 3 / 2];

        ByteBuffer yBuffer = planes[0].getBuffer().duplicate();
        ByteBuffer uBuffer = planes[1].getBuffer().duplicate();
        ByteBuffer vBuffer = planes[2].getBuffer().duplicate();

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        int outIndex = 0;
        for (int row = 0; row < height; row++) {
            int rowOffset = row * yRowStride;
            for (int col = 0; col < width; col++) {
                nv21[outIndex++] = yBuffer.get(rowOffset + col * yPixelStride);
            }
        }

        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        for (int row = 0; row < chromaHeight; row++) {
            int uRowOffset = row * uRowStride;
            int vRowOffset = row * vRowStride;
            for (int col = 0; col < chromaWidth; col++) {
                // NV21은 VU 순서다.
                nv21[outIndex++] = vBuffer.get(vRowOffset + col * vPixelStride);
                nv21[outIndex++] = uBuffer.get(uRowOffset + col * uPixelStride);
            }
        }

        return nv21;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (bitmap == null || rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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

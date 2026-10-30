package com.sm.myapplication.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.components.containers.Landmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

import java.io.InputStream;

public class GestureRecognizerHelper {

    private static final String TAG = "GestureHelper";
    private static final String MODEL_NAME = "gesture_recognizer.task";

    private GestureRecognizer gestureRecognizer;
    private String lastErrorMessage = "not initialized";
    private String lastAssetCheckMessage = "not checked";

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public String getLastAssetCheckMessage() {
        return lastAssetCheckMessage;
    }

    public boolean init(Context context) {
        close();

        try {
            Context appContext = context.getApplicationContext();

            // 1) 가장 먼저 assets 안에 모델이 실제로 들어갔는지 확인한다.
            //    이 단계가 실패하면 setModelAssetPath() 전에 원인을 화면에 보여줄 수 있다.
            try (InputStream inputStream = appContext.getAssets().open(MODEL_NAME)) {
                lastAssetCheckMessage = "asset OK, bytes=" + inputStream.available();
            }

            // 2) MediaPipe 공식 Android 예제와 동일하게 assets 루트 기준 파일명만 넣는다.
            BaseOptions baseOptions =
                    BaseOptions.builder()
                            .setModelAssetPath(MODEL_NAME)
                            .build();

            // VIDEO is synchronous; FrameAnalyzer runs it on a separate bounded worker,
            // never on CameraX's face-analysis executor.
            GestureRecognizer.GestureRecognizerOptions options =
                    GestureRecognizer.GestureRecognizerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.VIDEO)
                            .setNumHands(1)
                            // Restore the reference project's model thresholds.
                            .setMinHandDetectionConfidence(0.5f)
                            .setMinHandPresenceConfidence(0.5f)
                            .setMinTrackingConfidence(0.5f)
                            .build();

            gestureRecognizer = GestureRecognizer.createFromOptions(appContext, options);
            lastErrorMessage = "none";
            Log.d(TAG, "GestureRecognizer init success / " + lastAssetCheckMessage);
            return true;
        } catch (Throwable t) {
            lastErrorMessage = t.getClass().getSimpleName() + " / " + String.valueOf(t.getMessage());
            Log.e(TAG, "GestureRecognizer init failed / " + lastAssetCheckMessage, t);
            gestureRecognizer = null;
            return false;
        }
    }

    public boolean isReady() {
        return gestureRecognizer != null;
    }

    /** Empty means no supplemental command; strong model commands always win. */
    public static String handPoseFallbackLabel(GestureRecognizerResult result, String label, float score) {
        if (result == null || !FistPoseFallback.shouldTry(label, score)
                || result.worldLandmarks().isEmpty()) return "";
        float[][] points = firstHandWorldPoints(result);
        if (FistPoseFallback.isClosedFist(points)) return "Closed_Fist";
        if (FistPoseFallback.shouldTryPalm(label, score)
                && FistPoseFallback.isOpenPalm(points)) return "Open_Palm";
        return "";
    }

    /** Compatibility wrapper used by the fist-specific model regression test. */
    public static boolean hasClosedFistPose(GestureRecognizerResult result, String label, float score) {
        if (result == null || !FistPoseFallback.shouldTry(label, score)
                || result.worldLandmarks().isEmpty()) return false;
        return FistPoseFallback.isClosedFist(firstHandWorldPoints(result));
    }

    private static float[][] firstHandWorldPoints(GestureRecognizerResult result) {
        java.util.List<Landmark> landmarks = result.worldLandmarks().get(0);
        if (landmarks.size() != 21) return null;
        float[][] points = new float[21][3];
        for (int index = 0; index < 21; index++) {
            Landmark point = landmarks.get(index);
            points[index][0] = point.x();
            points[index][1] = point.y();
            points[index][2] = point.z();
        }
        return points;
    }

    public GestureRecognizerResult recognize(Bitmap bitmap, long timestampMs) {
        if (gestureRecognizer == null) {
            lastErrorMessage = "recognizer is null";
            return null;
        }
        if (bitmap == null) {
            lastErrorMessage = "bitmap is null";
            return null;
        }

        try {
            // MPImage.close() recycles its bitmap. Give it an owned copy because the
            // same analysis frame is still needed by the face detector after this call.
            Bitmap inputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);

            MPImage mpImage = new BitmapImageBuilder(inputBitmap).build();
            try {
                GestureRecognizerResult result = gestureRecognizer.recognizeForVideo(mpImage, timestampMs);
                lastErrorMessage = "none";
                return result;
            } finally {
                mpImage.close();
            }
        } catch (Throwable t) {
            lastErrorMessage = t.getClass().getSimpleName() + " / " + String.valueOf(t.getMessage());
            Log.e(TAG, "GestureRecognizer recognize failed", t);
            return null;
        }
    }

    public void close() {
        try {
            if (gestureRecognizer != null) {
                gestureRecognizer.close();
                gestureRecognizer = null;
            }
        } catch (Throwable t) {
            lastErrorMessage = t.getClass().getSimpleName() + " / " + String.valueOf(t.getMessage());
            Log.e(TAG, "GestureRecognizer close failed", t);
        }
    }
}

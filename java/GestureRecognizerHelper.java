package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
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

            // ImageAnalysis 분석 스레드에서 200ms 간격으로 동기 호출하므로 VIDEO 모드를 유지한다.
            // 추후 완전한 카메라 스트림 비동기 구조로 바꿀 때는 LIVE_STREAM + resultListener 방식으로 전환 가능하다.
            GestureRecognizer.GestureRecognizerOptions options =
                    GestureRecognizer.GestureRecognizerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.VIDEO)
                            .setNumHands(1)
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
            // MediaPipe 예제는 ARGB_8888 비트맵을 넘긴다. 다른 config면 복사해서 맞춘다.
            Bitmap inputBitmap = bitmap;
            if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                inputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            }

            MPImage mpImage = new BitmapImageBuilder(inputBitmap).build();
            GestureRecognizerResult result = gestureRecognizer.recognizeForVideo(mpImage, timestampMs);
            lastErrorMessage = "none";
            return result;
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

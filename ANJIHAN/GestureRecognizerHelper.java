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

public class GestureRecognizerHelper {

    private static final String TAG = "GestureHelper";

    private GestureRecognizer gestureRecognizer;

    public boolean init(Context context) {
        try {
            BaseOptions baseOptions =
                    BaseOptions.builder()
                            .setModelAssetPath("gesture_recognizer.task")
                            .build();

            GestureRecognizer.GestureRecognizerOptions options =
                    GestureRecognizer.GestureRecognizerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.VIDEO)
                            .setNumHands(1)
                            .setMinHandDetectionConfidence(0.5f)
                            .setMinHandPresenceConfidence(0.5f)
                            .setMinTrackingConfidence(0.5f)
                            .build();

            gestureRecognizer = GestureRecognizer.createFromOptions(context, options);
            Log.d(TAG, "GestureRecognizer init success");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "GestureRecognizer init failed", t);
            gestureRecognizer = null;
            return false;
        }
    }

    public boolean isReady() {
        return gestureRecognizer != null;
    }

    public GestureRecognizerResult recognize(Bitmap bitmap, long timestampMs) {
        if (gestureRecognizer == null || bitmap == null) return null;

        try {
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            return gestureRecognizer.recognizeForVideo(mpImage, timestampMs);
        } catch (Throwable t) {
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
            Log.e(TAG, "GestureRecognizer close failed", t);
        }
    }
}
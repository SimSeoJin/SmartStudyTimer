package com.sm.myapplication.vision;

import android.content.Context;
import android.content.SharedPreferences;

public final class PureModeUserSettings {
    public static final String MODE_BATTERY_SAVER = "BATTERY_SAVER";
    public static final String MODE_ACCURACY = "ACCURACY";

    public static final long FACE_AWAY_TIMEOUT_3S = 3_000L;
    public static final long FACE_AWAY_TIMEOUT_5S = 5_000L;
    public static final long FACE_AWAY_TIMEOUT_10S = 10_000L;

    private static final String PREF_NAME = "pure_mode_user_settings";
    private static final String KEY_FACE_AWAY_TIMEOUT = "faceAwayTimeout";
    private static final String KEY_GESTURE_RECOGNITION_ENABLED = "gestureRecognitionEnabled";
    private static final String KEY_RECOGNITION_MODE = "recognitionMode";

    private static final long DEFAULT_FACE_AWAY_TIMEOUT = FACE_AWAY_TIMEOUT_5S;
    private static final boolean DEFAULT_GESTURE_RECOGNITION_ENABLED = true;
    private static final String DEFAULT_RECOGNITION_MODE = MODE_ACCURACY;

    private PureModeUserSettings() {
    }

    public static Snapshot load(Context context) {
        SharedPreferences prefs = preferences(context);
        long faceAwayTimeoutMs = sanitizeFaceAwayTimeout(
                prefs.getLong(KEY_FACE_AWAY_TIMEOUT, DEFAULT_FACE_AWAY_TIMEOUT)
        );
        boolean gestureRecognitionEnabled = prefs.getBoolean(
                KEY_GESTURE_RECOGNITION_ENABLED,
                DEFAULT_GESTURE_RECOGNITION_ENABLED
        );
        String recognitionMode = sanitizeRecognitionMode(
                prefs.getString(KEY_RECOGNITION_MODE, DEFAULT_RECOGNITION_MODE)
        );

        return new Snapshot(faceAwayTimeoutMs, gestureRecognitionEnabled, recognitionMode);
    }

    public static Snapshot saveFaceAwayTimeout(Context context, long timeoutMs) {
        preferences(context)
                .edit()
                .putLong(KEY_FACE_AWAY_TIMEOUT, sanitizeFaceAwayTimeout(timeoutMs))
                .apply();
        return load(context);
    }

    public static Snapshot saveGestureRecognitionEnabled(Context context, boolean enabled) {
        preferences(context)
                .edit()
                .putBoolean(KEY_GESTURE_RECOGNITION_ENABLED, enabled)
                .apply();
        return load(context);
    }

    public static Snapshot saveRecognitionMode(Context context, String mode) {
        preferences(context)
                .edit()
                .putString(KEY_RECOGNITION_MODE, sanitizeRecognitionMode(mode))
                .apply();
        return load(context);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static long sanitizeFaceAwayTimeout(long timeoutMs) {
        if (timeoutMs == FACE_AWAY_TIMEOUT_3S || timeoutMs == FACE_AWAY_TIMEOUT_10S) {
            return timeoutMs;
        }
        return FACE_AWAY_TIMEOUT_5S;
    }

    private static String sanitizeRecognitionMode(String mode) {
        if (MODE_BATTERY_SAVER.equals(mode)) {
            return MODE_BATTERY_SAVER;
        }
        return MODE_ACCURACY;
    }

    public static final class Snapshot {
        public final long faceAwayTimeoutMs;
        public final boolean gestureRecognitionEnabled;
        public final String recognitionMode;
        public final long faceAnalyzeIntervalMs;
        public final long gestureAnalyzeIntervalMs;

        private Snapshot(long faceAwayTimeoutMs, boolean gestureRecognitionEnabled, String recognitionMode) {
            this.faceAwayTimeoutMs = faceAwayTimeoutMs;
            this.gestureRecognitionEnabled = gestureRecognitionEnabled;
            this.recognitionMode = recognitionMode;
            if (MODE_BATTERY_SAVER.equals(recognitionMode)) {
                this.faceAnalyzeIntervalMs = 800L;
                this.gestureAnalyzeIntervalMs = 300L;
            } else {
                this.faceAnalyzeIntervalMs = 300L;
                this.gestureAnalyzeIntervalMs = 100L;
            }
        }

        public boolean isBatterySaver() {
            return MODE_BATTERY_SAVER.equals(recognitionMode);
        }
    }
}

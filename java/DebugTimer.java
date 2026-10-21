package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;

public class DebugTimer {

    public interface OnTickListener {
        void onTick(long elapsedMillis);
    }

    private long elapsedMillis = 0L;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OnTickListener listener;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                elapsedMillis += 1000;
                if (listener != null) {
                    listener.onTick(elapsedMillis);
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    public DebugTimer(OnTickListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (!running) {
            running = true;
            handler.post(tickRunnable);
        }
    }

    public void pause() {
        running = false;
        handler.removeCallbacks(tickRunnable);
    }

    public void reset() {
        pause();
        elapsedMillis = 0L;
        if (listener != null) {
            listener.onTick(elapsedMillis);
        }
    }

    public static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
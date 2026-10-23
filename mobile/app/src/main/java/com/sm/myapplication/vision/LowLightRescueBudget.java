package com.sm.myapplication.vision;

/** Optional low-light work must leave time for fresh face/gesture frames. */
public final class LowLightRescueBudget {
    private long lastAccurateCompletedAt = Long.MIN_VALUE;
    private long accurateDurationMs;
    private boolean accurateInFlight;

    public boolean canAttempt(long now, long capturedAt) {
        return now >= capturedAt && now - capturedAt < 750L;
    }

    public boolean canUseAccurate(long now, long faceIntervalMs) {
        // Do not keep repeating a heavy rescue on a slow device/session.
        // Enhanced FAST stays available; a new analyzer/session may try again.
        if (accurateDurationMs >= 750L) return false;
        long interval = Math.max(faceIntervalMs, Math.max(1_000L, Math.min(1_000L, accurateDurationMs) * 2L));
        return !accurateInFlight && (lastAccurateCompletedAt == Long.MIN_VALUE
                || (now >= lastAccurateCompletedAt && now - lastAccurateCompletedAt >= interval));
    }

    public void accurateStarted() { accurateInFlight = true; }
    public void accurateFinished(long completedAt, long durationMs) {
        lastAccurateCompletedAt = completedAt;
        accurateDurationMs = Math.max(0L, durationMs);
        accurateInFlight = false;
    }
}

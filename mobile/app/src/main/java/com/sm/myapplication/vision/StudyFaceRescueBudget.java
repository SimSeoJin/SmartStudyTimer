package com.sm.myapplication.vision;

/**
 * Throttles current-frame original-face rescue without permanently disabling it
 * after a slow inference. This gates starts; it is not an inference timeout.
 */
public final class StudyFaceRescueBudget {
    private long lastAccurateCompletedAt = Long.MIN_VALUE;
    private boolean accurateInFlight;

    public boolean canAttempt(long now, long capturedAt) {
        return capturedAt >= 0L && now >= capturedAt && now - capturedAt < 750L;
    }

    public boolean canUseAccurate(long now, long faceIntervalMs) {
        long interval = Math.max(500L, faceIntervalMs);
        return now >= 0L && !accurateInFlight
                && (lastAccurateCompletedAt == Long.MIN_VALUE
                || (now >= lastAccurateCompletedAt
                && now - lastAccurateCompletedAt >= interval));
    }

    public void accurateStarted() {
        accurateInFlight = true;
    }

    public void accurateFinished(long completedAt) {
        lastAccurateCompletedAt = completedAt;
        accurateInFlight = false;
    }
}

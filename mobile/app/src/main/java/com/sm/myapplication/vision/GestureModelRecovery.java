package com.sm.myapplication.vision;

/** Retry policy owned by the gesture worker, independent of face/timer state. */
final class GestureModelRecovery {
    static final long RETRY_DELAY_MS = 2_000L;
    static final int FAILURE_LIMIT = 3;
    private volatile boolean ready;
    private volatile long retryAt = Long.MIN_VALUE;
    private volatile int initializationAttempts;
    private int consecutiveFailures;

    boolean canInitialize(long now) {
        return !ready && (retryAt == Long.MIN_VALUE || now >= retryAt);
    }

    void onInitializationResult(boolean success, long now) {
        initializationAttempts++;
        ready = success;
        consecutiveFailures = 0;
        retryAt = success ? Long.MIN_VALUE : now + RETRY_DELAY_MS;
    }

    /** A non-null result with zero hands is SUCCESS, not an inference failure. */
    boolean onInferenceResult(boolean success, long now) {
        if (!ready) return false;
        if (success) {
            consecutiveFailures = 0;
            return false;
        }
        if (++consecutiveFailures < FAILURE_LIMIT) return false;
        ready = false;
        consecutiveFailures = 0;
        retryAt = now + RETRY_DELAY_MS;
        return true;
    }

    boolean isReady() { return ready; }
    int getInitializationAttempts() { return initializationAttempts; }
}

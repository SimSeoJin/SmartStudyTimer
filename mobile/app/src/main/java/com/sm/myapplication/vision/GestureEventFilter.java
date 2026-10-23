package com.sm.myapplication.vision;

/** Original model-only confirmation policy, with ordered-sample validation. */
public final class GestureEventFilter {
    private static final float ACTION_SCORE = 0.55f;
    private static final long EVENT_COOLDOWN_MS = 1_500L;

    /** App controls: open palm pauses; closed fist resumes. */
    public static boolean isResumeCommand(String event) { return "Closed_Fist".equals(event); }
    public static boolean isPauseCommand(String event) { return "Open_Palm".equals(event); }

    private String candidate = "";
    private int count;
    private long lastSample = Long.MIN_VALUE;
    private long lastEvent = Long.MIN_VALUE;

    public String update(String label, float score, long now) {
        // Replayed or out-of-order frames cannot count as independent observations.
        // FrameAnalyzer separately rejects stale asynchronous inference results.
        if (lastSample != Long.MIN_VALUE && now <= lastSample) return "";
        lastSample = now;
        boolean action = "Open_Palm".equals(label) || "Closed_Fist".equals(label);
        if (!action || Float.isNaN(score) || Float.isInfinite(score)
                || score < ACTION_SCORE || score > 1f) {
            clearCandidate();
            return "";
        }
        if (!label.equals(candidate)) {
            candidate = label;
            count = 0;
        }
        count = Math.min(2, count + 1);
        // Both commands share one cooldown. A held gesture may confirm again
        // after it; there is no same-action latch or single-frame fast path.
        if (count >= 2 && (lastEvent == Long.MIN_VALUE || now - lastEvent >= EVENT_COOLDOWN_MS)) {
            lastEvent = now;
            clearCandidate();
            return label;
        }
        return "";
    }

    public void reset() {
        clearCandidate();
        lastSample = lastEvent = Long.MIN_VALUE;
    }

    private void clearCandidate() {
        candidate = "";
        count = 0;
    }
}

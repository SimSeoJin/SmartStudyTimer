package com.sm.myapplication.vision;

/** Lower confidence is justified by real image-boundary clipping, not by skin colour. */
public final class PartialFaceAcceptance {
    static final float MIN_EDGE_CONFIDENCE = 0.30f;
    private static final float MIN_INTERIOR_CONFIDENCE = 0.60f;

    private PartialFaceAcceptance() { }

    public static boolean accepts(float score, float left, float top, float right,
                                  float bottom, int width, int height) {
        for (float value : new float[]{score, left, top, right, bottom}) {
            if (Float.isNaN(value) || Float.isInfinite(value)) return false;
        }
        if (score > 1f || width <= 0 || height <= 0 || right <= left || bottom <= top
                || right <= 0f || bottom <= 0f || left >= width || top >= height) return false;
        float margin = Math.max(2f, Math.min(right - left, bottom - top) * .05f);
        boolean clipped = left <= margin || top <= margin
                || right >= width - margin || bottom >= height - margin;
        // Public hard-negative calibration: a fist can score >.5 as a fully
        // visible face. Do not solve partial-face recall by accepting that.
        return score >= (clipped ? MIN_EDGE_CONFIDENCE : MIN_INTERIOR_CONFIDENCE);
    }
}

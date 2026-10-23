package com.sm.myapplication.vision;

import java.util.Arrays;

/** Adaptive shadow enhancement, independent of Android for JVM regression tests. */
public final class LowLightEnhancer {
    private static final int DETAIL_FLOOR = 8;
    private static final int SHADOW_LIMIT = 120;
    private static final int TARGET_LUMA = 132;
    private static final int SHADOW_ANCHOR = 96;
    private static final int HIGHLIGHT_ANCHOR = 176;
    private static final double MIN_SHADOW_FRACTION = 0.08;
    private final int[] histogram = new int[256];
    private final int[] tone = new int[256];
    private int[] scratch = new int[0];
    private byte[] luminance = new byte[0];
    private final int[] gainQ16 = new int[256];
    private double gamma = Double.NaN;
    private double measuredLuma;
    private double shadowFraction;
    private boolean usableDetail;
    private int measuredContrast;

    public void reset() { gamma = Double.NaN; measuredLuma = 0; shadowFraction = 0; usableDetail = false; measuredContrast = 0; }
    public double getGamma() { return Double.isNaN(gamma) ? 1 : gamma; }
    public double getMeasuredLuma() { return measuredLuma; }
    public double getShadowFraction() { return shadowFraction; }
    public boolean hasUsableDetail() { return usableDetail; }
    public int getMeasuredContrast() { return measuredContrast; }

    /** Modifies ARGB pixels in place; ROI is a recent face or the central study area. */
    public boolean enhance(int[] pixels, int width, int height,
                           int left, int top, int right, int bottom) {
        if (width <= 0 || height <= 0 || (long) width * height > pixels.length) {
            throw new IllegalArgumentException("Invalid image size");
        }
        left = Math.max(0, Math.min(left, width - 1));
        top = Math.max(0, Math.min(top, height - 1));
        right = Math.max(left + 1, Math.min(right, width));
        bottom = Math.max(top + 1, Math.min(bottom, height));
        Arrays.fill(histogram, 0);
        int step = Math.max(1, Math.min(right - left, bottom - top) / 64);
        int count = 0;
        long total = 0;
        for (int y = top; y < bottom; y += step) {
            for (int x = left; x < right; x += step) {
                int value = luma(pixels[y * width + x]);
                histogram[value]++; total += value; count++;
            }
        }
        int median = 0, accumulated = 0;
        for (; median < 255; median++) {
            accumulated += histogram[median];
            if (accumulated >= (count + 1) / 2) break;
        }
        // A median alone misses an initially undetected dark face in a bright scene.
        // Ignore near-black pixels and isolated specks; use the darkest meaningful
        // fraction, without claiming that a dark patch is itself a detected face.
        measuredLuma = 0.65 * median + 0.35 * total / count;
        int shadows = 0;
        long shadowTotal = 0;
        for (int value = DETAIL_FLOOR; value <= SHADOW_LIMIT; value++) {
            shadows += histogram[value];
            shadowTotal += (long) value * histogram[value];
        }
        shadowFraction = (double) shadows / count;
        int detailCount = count;
        for (int value = 0; value < DETAIL_FLOOR; value++) detailCount -= histogram[value];
        // Camera YUV/JPEG may encode a black scene as flat gray (~16). Brightness
        // alone is not evidence of useful detail. Ignore uniform fields and specks.
        measuredContrast = percentile(count, 0.95) - percentile(count, 0.05);
        usableDetail = detailCount >= Math.max(1, (int) Math.ceil(count * 0.02))
                && measuredContrast >= 8;
        if (!usableDetail) {
            // Near-black sensor noise is not useful facial detail. Leave it alone
            // and let the actual detector decide, rather than advertise fake gain.
            gamma = Double.NaN;
            return false;
        }
        if (shadows >= Math.max(4, (int) Math.ceil(count * MIN_SHADOW_FRACTION))) {
            int targetCount = Math.max(1, (shadows + 1) / 2);
            int shadowMedian = DETAIL_FLOOR;
            accumulated = 0;
            for (; shadowMedian < SHADOW_LIMIT; shadowMedian++) {
                accumulated += histogram[shadowMedian];
                if (accumulated >= targetCount) break;
            }
            double subjectLuma = 0.75 * shadowMedian + 0.25 * shadowTotal / shadows;
            measuredLuma = Math.min(measuredLuma, subjectLuma);
        }
        double desired = measuredLuma >= TARGET_LUMA ? 1 : Math.max(0.45,
                Math.log(TARGET_LUMA / 255.0) / Math.log(Math.max(1, measuredLuma) / 255));
        gamma = Double.isNaN(gamma) || desired == 1 ? desired : gamma * 0.7 + desired * 0.3;
        if (gamma >= 0.99) return false;
        // A monotonic shadow curve that joins the identity at 176. Bright monitor
        // pixels stay unchanged instead of washing out the entire analysis image.
        int liftedAnchor = Math.min(SHADOW_ANCHOR * 4,
                (int) Math.round(255 * Math.pow(SHADOW_ANCHOR / 255.0, gamma)));
        for (int value = 0; value < 256; value++) {
            if (value <= SHADOW_ANCHOR) {
                // Keep black black; bounded gain cannot invent missing sensor detail.
                tone[value] = Math.min(value * 4,
                        (int) Math.round(255 * Math.pow(value / 255.0, gamma)));
            } else if (value < HIGHLIGHT_ANCHOR) {
                tone[value] = (int) Math.round(liftedAnchor
                        + (HIGHLIGHT_ANCHOR - liftedAnchor)
                        * (value - SHADOW_ANCHOR) / (double) (HIGHLIGHT_ANCHOR - SHADOW_ANCHOR));
            } else {
                tone[value] = value;
            }
        }
        int length = width * height;
        if (scratch.length != length) scratch = new int[length];
        if (luminance.length != length) luminance = new byte[length];
        for (int i = 0; i < length; i++) luminance[i] = (byte) luma(pixels[i]);
        gainQ16[0] = 1 << 16;
        for (int i = 1; i < 256; i++) gainQ16[i] = (tone[i] << 16) / i;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x, color = pixels[index];
                int originalLuma = luminance[index] & 255;
                if (originalLuma >= HIGHLIGHT_ANCHOR) {
                    scratch[index] = color;
                    continue;
                }
                int r = red(color), g = green(color), b = blue(color);
                // Spatial, edge-aware shadow denoising; no temporal face/hand ghosting.
                if (originalLuma < 100 && x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                    int sumR = r * 4, sumG = g * 4, sumB = b * 4, weight = 4;
                    for (int n = 0; n < 4; n++) {
                        int offset = n == 0 ? -1 : n == 1 ? 1 : n == 2 ? -width : width;
                        int neighbor = pixels[index + offset];
                        if (Math.abs((luminance[index + offset] & 255) - originalLuma) <= 10) {
                            sumR += red(neighbor); sumG += green(neighbor); sumB += blue(neighbor);
                            weight++;
                        }
                    }
                    r = sumR / weight; g = sumG / weight; b = sumB / weight;
                }
                int light = (299 * r + 587 * g + 114 * b + 500) / 1000;
                int gain = gainQ16[light];
                int maxChannel = Math.max(r, Math.max(g, b));
                if (maxChannel > 0 && maxChannel * (long) gain > (255L << 16)) gain = (255 << 16) / maxChannel;
                scratch[index] = (color & 0xff000000)
                        | (((r * gain + 32768) >> 16) << 16)
                        | (((g * gain + 32768) >> 16) << 8)
                        | ((b * gain + 32768) >> 16);
            }
        }
        System.arraycopy(scratch, 0, pixels, 0, length);
        return true;
    }

    private static int red(int color) { return (color >>> 16) & 255; }
    private int percentile(int count, double fraction) {
        int target = Math.max(1, (int) Math.ceil(count * fraction));
        int accumulated = 0;
        for (int value = 0; value < histogram.length; value++) {
            accumulated += histogram[value];
            if (accumulated >= target) return value;
        }
        return 255;
    }
    private static int green(int color) { return (color >>> 8) & 255; }
    private static int blue(int color) { return color & 255; }
    private static int luma(int color) {
        return (299 * red(color) + 587 * green(color) + 114 * blue(color) + 500) / 1000;
    }
}

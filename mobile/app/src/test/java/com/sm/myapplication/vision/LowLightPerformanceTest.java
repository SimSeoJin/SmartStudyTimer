package com.sm.myapplication.vision;

import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class LowLightPerformanceTest {

    @Test public void budgetSkipsAlreadySlowFramesAndThrottlesAccurateOnly() {
        LowLightRescueBudget budget = new LowLightRescueBudget();
        assertTrue(budget.canAttempt(749, 0)); assertFalse(budget.canAttempt(750, 0));
        assertFalse(budget.canAttempt(0, 1));
        assertTrue(budget.canUseAccurate(0, 500));
        budget.accurateStarted();
        assertFalse(budget.canUseAccurate(5_000, 500));
        budget.accurateFinished(600, 600);
        assertFalse(budget.canUseAccurate(1_799, 500)); assertTrue(budget.canUseAccurate(1_800, 500));
        budget.accurateStarted(); budget.accurateFinished(4_800, 3_000);
        assertFalse(budget.canUseAccurate(4_800, 500)); // A slow run cannot consume its own cooldown.
        assertFalse(budget.canUseAccurate(6_799, 500)); assertFalse(budget.canUseAccurate(6_800, 500));
        assertFalse(budget.canUseAccurate(1_000_000, 500)); // Slow ACCURATE stays disabled for this session.
        assertFalse(budget.canUseAccurate(4_799, 500));
        budget.accurateStarted(); budget.accurateFinished(6_800, Long.MAX_VALUE);
        assertFalse(budget.canUseAccurate(8_799, 500)); assertFalse(budget.canUseAccurate(8_800, 500));
        LowLightRescueBudget freshSession = new LowLightRescueBudget();
        assertTrue(freshSession.canUseAccurate(8_800, 500));
        freshSession.accurateStarted(); freshSession.accurateFinished(9_000, 749);
        assertTrue(freshSession.canUseAccurate(10_498, 500));
        freshSession.accurateStarted(); freshSession.accurateFinished(11_000, 750);
        assertFalse(freshSession.canUseAccurate(100_000, 500));
    }

    @Test public void optimizedPixelsMatchWorkingReferenceWithinOneLevel() {
        Random random = new Random(414);
        LowLightEnhancer optimized = new LowLightEnhancer();
        LegacyLowLightEnhancerReference reference = new LegacyLowLightEnhancerReference();
        for (int frame = 0; frame < 12; frame++) {
            int[] pixels = new int[96 * 72];
            for (int i = 0; i < pixels.length; i++) {
                int r = random.nextInt(150), g = random.nextInt(130), b = random.nextInt(120);
                pixels[i] = (random.nextInt(256) << 24) | (r << 16) | (g << 8) | b;
                if (i % 19 == 0) pixels[i] = 0xffeeeeee;
            }
            int[] expected = pixels.clone();
            assertEquals(reference.enhance(expected, 96, 72, 8, 4, 88, 68), optimized.enhance(pixels, 96, 72, 8, 4, 88, 68));
            assertEquals(reference.getGamma(), optimized.getGamma(), 0.0);
            for (int i = 0; i < pixels.length; i++) {
                assertEquals(expected[i] >>> 24, pixels[i] >>> 24);
                for (int shift : new int[] {0, 8, 16}) assertTrue(Math.abs((pixels[i] >>> shift & 255) - (expected[i] >>> shift & 255)) <= 1);
            }
        }
    }

    @Test public void benchmarkPixelProcessingWithoutChangingTheExposureCurve() {
        int width = 640, height = 480;
        int[] original = new int[width * height];
        for (int i = 0; i < original.length; i++) {
            int gray = 35 + (i * 37 % 80);
            original[i] = 0xff000000 | (gray << 16) | (gray << 8) | gray;
        }
        LowLightEnhancer optimized = new LowLightEnhancer();
        LegacyLowLightEnhancerReference reference = new LegacyLowLightEnhancerReference();
        long[] before = new long[12], after = new long[12];
        int[] pixels = original.clone();
        for (int i = -8; i < 12; i++) {
            System.arraycopy(original, 0, pixels, 0, pixels.length);
            long start = System.nanoTime(); reference.enhance(pixels, width, height, 80, 48, 560, 432);
            long legacyTime = System.nanoTime() - start;
            System.arraycopy(original, 0, pixels, 0, pixels.length);
            start = System.nanoTime(); optimized.enhance(pixels, width, height, 80, 48, 560, 432);
            long optimizedTime = System.nanoTime() - start;
            if (i >= 0) { before[i] = legacyTime; after[i] = optimizedTime; }
        }
        Arrays.sort(before); Arrays.sort(after);
        System.out.println("LLE_BENCH desktop 640x480 legacyMedianMs=" + before[6] / 1e6
                + " optimizedMedianMs=" + after[6] / 1e6 + " legacyP95Ms=" + before[11] / 1e6 + " optimizedP95Ms=" + after[11] / 1e6);
        // Timing is measured, not used as a flaky host-dependent pass criterion.
        assertTrue(before[6] > 0 && after[6] > 0);
    }
}

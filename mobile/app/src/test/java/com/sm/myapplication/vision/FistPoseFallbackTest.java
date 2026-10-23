package com.sm.myapplication.vision;

import org.junit.Test;

import static org.junit.Assert.*;

public class FistPoseFallbackTest {
    @Test public void canonicalFistPassesButOpenPalmDoesNot() {
        assertTrue(FistPoseFallback.isClosedFist(fist()));
        float[][] open = fist();
        for (int base : new int[]{5, 9, 13, 17}) extend(open, base);
        assertFalse(FistPoseFallback.isClosedFist(open));
    }

    @Test public void everySingleExtendedFingerRejectsFist() {
        for (int base : new int[]{5, 9, 13, 17}) {
            float[][] points = fist();
            extend(points, base);
            assertFalse("Extended finger at " + base, FistPoseFallback.isClosedFist(points));
        }
    }

    @Test public void bentFingersWithExtendedThumbAreNotFist() {
        float[][] points = fist();
        points[3] = new float[]{-.85f, 1.30f, 0f};
        points[4] = new float[]{-.85f, 1.95f, 0f};
        assertFalse(FistPoseFallback.isClosedFist(points));
    }

    @Test public void rotationReflectionTranslationAndScaleDoNotChangeTheDecision() {
        float[][] palm = fist();
        for (int base : new int[]{5, 9, 13, 17}) extend(palm, base);
        for (float scale : new float[]{.005f, .1f, 1f, 100f}) {
            for (boolean mirror : new boolean[]{false, true}) {
                assertTrue(FistPoseFallback.isClosedFist(transform(fist(), scale, mirror)));
                assertFalse(FistPoseFallback.isClosedFist(transform(palm, scale, mirror)));
            }
        }
    }

    @Test public void notEnoughPointsOrDimensionsCannotBeFist() {
        assertFalse(FistPoseFallback.isClosedFist(null));
        assertFalse(FistPoseFallback.isClosedFist(new float[20][3]));
        float[][] points = fist();
        points[12] = null;
        assertFalse(FistPoseFallback.isClosedFist(points));
        points = fist();
        points[12] = new float[2];
        assertFalse(FistPoseFallback.isClosedFist(points));
    }

    @Test public void nonFiniteCoordinateAnywhereRejectsTheHand() {
        for (int index = 0; index < 21; index++) {
            for (float bad : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY}) {
                float[][] points = fist();
                points[index][index % 3] = bad;
                assertFalse(FistPoseFallback.isClosedFist(points));
            }
        }
    }

    @Test public void degeneratePalmsAndCoincidentFingerJointsCannotPass() {
        assertFalse(FistPoseFallback.isClosedFist(new float[21][3]));
        float[][] points = fist();
        points[17] = points[5].clone();
        assertFalse(FistPoseFallback.isClosedFist(points));
        for (int base : new int[]{5, 9, 13, 17}) {
            points = fist();
            points[base + 1] = points[base].clone();
            assertFalse(FistPoseFallback.isClosedFist(points));
        }
        points = fist();
        points[4] = points[3].clone();
        assertFalse(FistPoseFallback.isClosedFist(points));
    }

    @Test public void fullyStraightButShortFingerCannotPassByDistanceAlone() {
        float[][] points = fist();
        points[6] = new float[]{-.38f, 1.00f, 0f};
        points[7] = new float[]{-.38f, 1.05f, 0f};
        points[8] = new float[]{-.38f, 1.10f, 0f};
        assertFalse(FistPoseFallback.isClosedFist(points));
    }

    @Test public void neutralAndUncertainCommandsMayUseFallback() {
        for (String label : new String[]{null, "", "None", "no_hand"}) {
            assertTrue(FistPoseFallback.shouldTry(label, .9f));
        }
        assertTrue(FistPoseFallback.shouldTry("Closed_Fist", .64f));
        assertTrue(FistPoseFallback.shouldTry("Open_Palm", .64f));
        assertFalse(FistPoseFallback.shouldTry("Closed_Fist", .65f));
        assertFalse(FistPoseFallback.shouldTry("Closed_Fist", .9f));
    }

    @Test public void fallbackNeverOverridesClearCompetingGestures() {
        for (String label : new String[]{"Open_Palm", "Thumb_Up", "Thumb_Down", "Pointing_Up",
                "Victory", "ILoveYou", "unknown_model_label"}) {
            assertFalse(FistPoseFallback.shouldTry(label, .9f));
            assertTrue(FistPoseFallback.shouldTry(label, .1f));
        }
    }

    @Test public void invalidScoresCannotInvokeFallback() {
        for (float score : new float[]{-.01f, -2f, 1.001f, Float.NaN,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
            for (String label : new String[]{null, "", "None", "no_hand", "Closed_Fist"}) {
                assertFalse(FistPoseFallback.shouldTry(label, score));
            }
        }
    }

    @Test public void missingCategorySentinelMayUseActualWorldLandmarks() {
        assertTrue(FistPoseFallback.shouldTry("no_hand", -1f));
        assertTrue(FistPoseFallback.shouldTry(null, -1f));
        assertTrue(FistPoseFallback.shouldTry("", -1f));
        assertFalse(FistPoseFallback.shouldTry("Closed_Fist", -1f));
        assertFalse(FistPoseFallback.shouldTry("None", -1f));
    }

    @Test public void thumbBesideRelaxedFistNeedNotBeTuckedIntoPalm() {
        float[][] points = fist();
        points[3] = new float[]{-.90f, .75f, -.1f};
        points[4] = new float[]{-1.05f, 1.05f, -.1f};
        assertTrue(FistPoseFallback.isClosedFist(points));
        assertFalse(FistPoseFallback.isOpenPalm(points));
    }

    @Test public void threeCurledAndOneUncertainFingerCanRescueRelaxedFist() {
        float[][] points = fist();
        int base = 17;
        points[base + 1] = offset(points[base], 0f, .38f, 0f);
        points[base + 2] = offset(points[base], 0f, .63f, -.02f);
        points[base + 3] = offset(points[base], 0f, .57f, -.20f);
        assertTrue(FistPoseFallback.isClosedFist(points));
        assertFalse(FistPoseFallback.isOpenPalm(points));
    }

    @Test public void openPalmAllowsFoldedThumbAndOneGentlyBentFinger() {
        float[][] points = palm();
        points[18] = offset(points[17], 0f, .38f, 0f);
        points[19] = offset(points[17], 0f, .63f, -.10f);
        points[20] = offset(points[17], 0f, .70f, -.25f);
        assertTrue(FistPoseFallback.isOpenPalm(points));
        assertFalse(FistPoseFallback.isClosedFist(points));
    }

    @Test public void tightlyCurledFingerAndVictoryCannotBecomePalm() {
        float[][] points = palm();
        float[][] folded = fist();
        for (int index = 17; index <= 20; index++) points[index] = folded[index].clone();
        assertFalse(FistPoseFallback.isOpenPalm(points));
        points = fist();
        extend(points, 5);
        extend(points, 9);
        assertFalse(FistPoseFallback.isOpenPalm(points));
        assertFalse(FistPoseFallback.isClosedFist(points));
    }

    @Test public void threeExtendedFingersCanRescueWeakPalmButNeverInventNeutralPause() {
        float[][] points = palm();
        points[18] = offset(points[17], 0f, .38f, 0f);
        points[19] = offset(points[17], 0f, .63f, -.02f);
        points[20] = offset(points[17], 0f, .57f, -.20f);
        assertTrue(FistPoseFallback.isOpenPalm(points));
        assertTrue(FistPoseFallback.shouldTryPalm("Open_Palm", .19f));
        assertFalse(FistPoseFallback.shouldTryPalm("None", .3f));
    }

    @Test public void straightNarrowRestingHandCannotInventPalmFromNeutralModelLabel() {
        float[][] points = palm();
        // Straight reconstructed chains alone cannot distinguish a resting hand.
        // Geometry is usable only when the model itself proposed Open_Palm.
        for (float[] point : points) point[0] *= .4f;
        assertTrue(FistPoseFallback.isOpenPalm(points));
        assertFalse(FistPoseFallback.shouldTryPalm("None", .833f));
        assertFalse(FistPoseFallback.shouldTryPalm("None", .3f));
        assertFalse(FistPoseFallback.shouldTryPalm("no_hand", -1f));
    }

    @Test public void clearNoneMustNotBeOverriddenBySupplementalPalm() {
        assertFalse(FistPoseFallback.shouldTryPalm("None", .65f));
        assertFalse(FistPoseFallback.shouldTryPalm("None", .833f));
        assertTrue("Fist recovery keeps its separate working policy",
                FistPoseFallback.shouldTry("None", .833f));
        assertFalse(FistPoseFallback.shouldTryPalm("None", .64f));
        assertTrue(FistPoseFallback.shouldTryPalm("Open_Palm", .19f));
        assertFalse(FistPoseFallback.shouldTryPalm("no_hand", -1f));
        assertFalse(FistPoseFallback.shouldTryPalm("Thumb_Up", .1f));
        assertFalse(FistPoseFallback.shouldTryPalm("Closed_Fist", .9f));
    }

    @Test public void openPalmIsRotationMirrorAndScaleInvariant() {
        for (float scale : new float[]{.005f, .1f, 1f, 100f}) {
            for (boolean mirror : new boolean[]{false, true}) {
                assertTrue(FistPoseFallback.isOpenPalm(transform(palm(), scale, mirror)));
                assertFalse(FistPoseFallback.isOpenPalm(transform(fist(), scale, mirror)));
            }
        }
    }

    @Test public void invalidHandsNeverProducePalmEither() {
        assertFalse(FistPoseFallback.isOpenPalm(null));
        assertFalse(FistPoseFallback.isOpenPalm(new float[21][3]));
        float[][] points = palm();
        points[9][2] = Float.NaN;
        assertFalse(FistPoseFallback.isOpenPalm(points));
        points = palm();
        points[6] = points[5].clone();
        assertFalse(FistPoseFallback.isOpenPalm(points));
    }

    private static float[][] palm() {
        float[][] points = fist();
        for (int base : new int[]{5, 9, 13, 17}) extend(points, base);
        return points;
    }

    private static float[][] fist() {
        float[][] points = new float[21][3];
        points[0] = new float[]{0f, 0f, 0f};
        points[1] = new float[]{-.45f, .15f, 0f};
        points[2] = new float[]{-.65f, .35f, -.05f};
        points[3] = new float[]{-.43f, .72f, -.22f};
        points[4] = new float[]{-.04f, .87f, -.16f};
        float[][] knuckles = {{-.38f, .95f, 0f}, {-.10f, 1.03f, 0f},
                {.18f, .97f, 0f}, {.42f, .82f, 0f}};
        int finger = 0;
        for (int base : new int[]{5, 9, 13, 17}) {
            points[base] = knuckles[finger++].clone();
            points[base + 1] = offset(points[base], 0f, .38f, -.10f);
            points[base + 2] = offset(points[base], 0f, .18f, -.38f);
            points[base + 3] = offset(points[base], 0f, -.05f, -.30f);
        }
        return points;
    }

    private static void extend(float[][] points, int base) {
        points[base + 1] = offset(points[base], 0f, .45f, 0f);
        points[base + 2] = offset(points[base], 0f, .73f, 0f);
        points[base + 3] = offset(points[base], 0f, .98f, 0f);
    }

    private static float[] offset(float[] point, float x, float y, float z) {
        return new float[]{point[0] + x, point[1] + y, point[2] + z};
    }

    private static float[][] transform(float[][] source, float scale, boolean mirror) {
        float[][] result = new float[source.length][3];
        double cx = Math.cos(.71), sx = Math.sin(.71);
        double cz = Math.cos(1.2), sz = Math.sin(1.2);
        for (int i = 0; i < source.length; i++) {
            double x = source[i][0] * (mirror ? -1 : 1);
            double y = source[i][1] * cx - source[i][2] * sx;
            double z = source[i][1] * sx + source[i][2] * cx;
            result[i][0] = (float) ((x * cz - y * sz + 2.5) * scale);
            result[i][1] = (float) ((x * sz + y * cz - 1.7) * scale);
            result[i][2] = (float) ((z + .6) * scale);
        }
        return result;
    }
}

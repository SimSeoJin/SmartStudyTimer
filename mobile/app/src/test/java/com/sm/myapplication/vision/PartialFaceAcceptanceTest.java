package com.sm.myapplication.vision;

import org.junit.Test;
import static org.junit.Assert.*;

public class PartialFaceAcceptanceTest {
    @Test public void weakButRealEdgeCandidatesCanPassOnEveryEdge() {
        assertTrue(accept(.30f, -40, 100, 80, 220));
        assertTrue(accept(.30f, 420, 100, 530, 220));
        assertTrue(accept(.30f, 180, -40, 300, 80));
        assertTrue(accept(.30f, 180, 590, 300, 710));
    }

    @Test public void fullFrameFistFalsePositivesCannotBecomeFaceEvidence() {
        assertFalse(PartialFaceAcceptance.accepts(.5053f, 173, 58, 264, 149, 358, 354));
        assertFalse(PartialFaceAcceptance.accepts(.5814f, 106, 42, 274, 210, 358, 354));
        assertFalse(PartialFaceAcceptance.accepts(.2502f, 80, 0, 281, 201, 358, 354));
    }

    @Test public void measuredHalfFacesRemainAccepted() {
        assertTrue(accept(.4826f, 0, 69, 118, 187));
        assertTrue(accept(.3430f, 341, 65, 491, 215));
    }

    @Test public void strongInteriorAndUpperFaceCanPassWithoutVisibleMouthCheck() {
        assertTrue(accept(.60f, 170, 80, 310, 220));
        assertTrue(accept(.64f, 173, 49, 286, 162));
        assertFalse(accept(.599f, 170, 80, 310, 220));
    }

    @Test public void oldQuarterCropBackgroundFalsePositiveIsRejected() {
        assertFalse(accept(.2588f, 278, 58, 425, 205));
    }

    @Test public void noOverlapAndInvalidGeometryAreNotEvidence() {
        assertFalse(accept(.99f, -100, 10, -1, 110));
        assertFalse(accept(.99f, 481, 10, 600, 110));
        assertFalse(accept(.99f, 20, 20, 20, 40));
        assertFalse(accept(.99f, 20, 40, 40, 20));
        assertFalse(PartialFaceAcceptance.accepts(.99f, 0, 0, 10, 10, 0, 640));
    }

    @Test public void nanInfinityAndOutOfRangeScoresCannotPass() {
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertFalse(accept(value, 0, 80, 140, 220));
            assertFalse(accept(.99f, value, 80, 140, 220));
        }
        assertFalse(accept(1.01f, 0, 80, 140, 220));
        assertFalse(accept(-.1f, 0, 80, 140, 220));
    }

    private static boolean accept(float score, float l, float t, float r, float b) {
        return PartialFaceAcceptance.accepts(score, l, t, r, b, 480, 640);
    }
}

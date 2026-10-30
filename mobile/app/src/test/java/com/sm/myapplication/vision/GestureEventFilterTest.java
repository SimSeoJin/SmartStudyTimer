package com.sm.myapplication.vision;

import org.junit.Test;
import static org.junit.Assert.*;

/** Reference confirmation/cooldown semantics with the app's palm-pause mapping. */
public class GestureEventFilterTest {
    @Test public void appControlsArePalmPauseAndFistResume() {
        assertTrue(GestureEventFilter.isPauseCommand("Open_Palm"));
        assertFalse(GestureEventFilter.isResumeCommand("Open_Palm"));
        assertTrue(GestureEventFilter.isResumeCommand("Closed_Fist"));
        assertFalse(GestureEventFilter.isPauseCommand("Closed_Fist"));
        assertFalse(GestureEventFilter.isResumeCommand("no_hand"));
        assertFalse(GestureEventFilter.isPauseCommand(null));
    }

    @Test public void evenPerfectScoreNeedsTwoIndependentSamples() {
        for (float score : new float[]{.55f, .65f, .99f, 1f}) {
            GestureEventFilter filter = new GestureEventFilter();
            assertEquals("", filter.update("Open_Palm", score, 0));
            assertEquals("Open_Palm", filter.update("Open_Palm", score, 200));
        }
    }

    @Test public void firstCommandDoesNotWaitForProcessUptimeCooldown() {
        GestureEventFilter filter = new GestureEventFilter();
        assertEquals("", filter.update("Closed_Fist", .9f, 0));
        assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 1));
    }

    @Test public void samplesBelowReferenceThresholdNeverAccumulate() {
        for (float score : new float[]{0f, .2f, .35f, .54999f}) {
            GestureEventFilter filter = new GestureEventFilter();
            for (int i = 0; i < 12; i++) assertEquals("", filter.update("Closed_Fist", score, i * 200L));
            assertEquals("", filter.update("Closed_Fist", .55f, 2400));
            assertEquals("Closed_Fist", filter.update("Closed_Fist", .55f, 2600));
        }
    }

    @Test public void heldGestureRepeatsAfterSharedCooldown() {
        GestureEventFilter filter = new GestureEventFilter();
        assertEquals("", filter.update("Open_Palm", .9f, 0));
        assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 200));
        assertEquals("", filter.update("Open_Palm", .9f, 400));
        assertEquals("", filter.update("Open_Palm", .9f, 600));
        assertEquals("", filter.update("Open_Palm", .9f, 1699));
        assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 1700));
        // An emission resets confirmation even if the next frame arrives much later.
        assertEquals("", filter.update("Open_Palm", .9f, 4000));
        assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 4200));
    }

    @Test public void oppositeGestureAlsoWaitsForSharedCooldown() {
        GestureEventFilter filter = new GestureEventFilter();
        filter.update("Open_Palm", .9f, 0);
        assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 200));
        assertEquals("", filter.update("Closed_Fist", .9f, 400));
        assertEquals("", filter.update("Closed_Fist", .9f, 600));
        assertEquals("", filter.update("Closed_Fist", .9f, 1699));
        assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 1700));
    }

    @Test public void cooldownRemainsAfterNeutralOrNoHand() {
        for (String interruption : new String[]{"None", "no_hand"}) {
            GestureEventFilter filter = new GestureEventFilter();
            filter.update("Open_Palm", .9f, 0);
            filter.update("Open_Palm", .9f, 200);
            assertEquals("", filter.update(interruption, "no_hand".equals(interruption) ? -1f : .9f, 400));
            assertEquals("", filter.update("Closed_Fist", .9f, 600));
            assertEquals("", filter.update("Closed_Fist", .9f, 800));
            assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 1700));
        }
    }

    @Test public void anyDropoutOrLowScoreDiscardsFirstConfirmation() {
        String[] labels = {"no_hand", "None", "Open_Palm", "Thumb_Up", null, ""};
        float[] scores = {-1f, .99f, .5499f, .99f, .99f, .99f};
        for (int i = 0; i < labels.length; i++) {
            GestureEventFilter filter = new GestureEventFilter();
            assertEquals("", filter.update("Open_Palm", .9f, 0));
            assertEquals("", filter.update(labels[i], scores[i], 200));
            assertEquals("", filter.update("Open_Palm", .9f, 400));
            assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 600));
        }
    }

    @Test public void switchingLabelStartsANewTwoSampleConfirmation() {
        GestureEventFilter filter = new GestureEventFilter();
        assertEquals("", filter.update("Open_Palm", .9f, 0));
        assertEquals("", filter.update("Closed_Fist", .9f, 200));
        assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 400));
    }

    @Test public void slowConsecutiveSamplesHaveNoNewConfirmationExpiry() {
        for (long gap : new long[]{900L, 3000L, 3001L, 10_000L}) {
            GestureEventFilter filter = new GestureEventFilter();
            assertEquals("", filter.update("Closed_Fist", .9f, 0));
            assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, gap));
        }
    }

    @Test public void duplicatesAndOldSamplesCannotCountAsConfirmation() {
        GestureEventFilter filter = new GestureEventFilter();
        assertEquals("", filter.update("Closed_Fist", .9f, 100));
        assertEquals("", filter.update("Closed_Fist", .9f, 100));
        assertEquals("", filter.update("Closed_Fist", .9f, 50));
        assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 250));
    }

    @Test public void oldOrDuplicateDropoutCannotEraseAcceptedConfirmation() {
        GestureEventFilter filter = new GestureEventFilter();
        filter.update("Open_Palm", .9f, 100);
        assertEquals("", filter.update("no_hand", -1f, 100));
        assertEquals("", filter.update("no_hand", -1f, 50));
        assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 300));
    }

    @Test public void invalidScoresDiscardConfirmation() {
        for (float score : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1f, 1.0001f}) {
            GestureEventFilter filter = new GestureEventFilter();
            filter.update("Open_Palm", .9f, 0);
            assertEquals("", filter.update("Open_Palm", score, 200));
            assertEquals("", filter.update("Open_Palm", .9f, 400));
            assertEquals("Open_Palm", filter.update("Open_Palm", .9f, 600));
        }
    }

    @Test public void nonCommandsNeverEmitEvenAtHighConfidence() {
        for (String label : new String[]{"None", "no_hand", "Thumb_Up", "Victory", null, ""}) {
            GestureEventFilter filter = new GestureEventFilter();
            for (int i = 0; i < 12; i++) assertEquals("", filter.update(label, 1f, i * 200L));
        }
    }

    @Test public void resetClearsConfirmationCooldownAndTransportOrdering() {
        GestureEventFilter filter = new GestureEventFilter();
        filter.update("Open_Palm", .9f, 1000);
        filter.update("Open_Palm", .9f, 1200);
        filter.reset();
        assertEquals("", filter.update("Closed_Fist", .9f, 0));
        assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 200));
    }

    @Test public void lowScoreDuringCooldownRequiresTwoFreshConfirmations() {
        GestureEventFilter filter = new GestureEventFilter();
        filter.update("Open_Palm", .9f, 0);
        filter.update("Open_Palm", .9f, 200);
        filter.update("Closed_Fist", .9f, 400);
        filter.update("Closed_Fist", .9f, 600);
        assertEquals("", filter.update("Closed_Fist", .5f, 1600));
        assertEquals("", filter.update("Closed_Fist", .9f, 1800));
        assertEquals("Closed_Fist", filter.update("Closed_Fist", .9f, 2000));
    }
}

package com.sm.myapplication.vision;

import org.junit.Test;

import static org.junit.Assert.*;

public class StudyFaceRescueBudgetTest {
    @Test public void firstFreshFrameMayStartImmediately() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        assertTrue(budget.canAttempt(0L, 0L));
        assertTrue(budget.canUseAccurate(0L, 500L));
        assertTrue(budget.canUseAccurate(10_000L, 800L));
    }

    @Test public void anotherAccurateRunCannotStartWhileInFlight() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        budget.accurateStarted();
        assertFalse(budget.canUseAccurate(0L, 500L));
        assertFalse(budget.canUseAccurate(100_000L, 500L));
    }

    @Test public void defaultCooldownStartsAtCompletionAndIncludesBoundary() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        budget.accurateStarted();
        budget.accurateFinished(5_000L);
        assertFalse(budget.canUseAccurate(5_000L, 500L));
        assertFalse(budget.canUseAccurate(5_499L, 500L));
        assertTrue(budget.canUseAccurate(5_500L, 500L));
    }

    @Test public void configuredLongerFaceIntervalControlsCooldown() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        budget.accurateStarted();
        budget.accurateFinished(1_000L);
        assertFalse(budget.canUseAccurate(1_500L, 800L));
        assertFalse(budget.canUseAccurate(1_799L, 800L));
        assertTrue(budget.canUseAccurate(1_800L, 800L));
    }

    @Test public void shorterFaceIntervalStillKeepsMinimumCooldown() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        budget.accurateStarted();
        budget.accurateFinished(1_000L);
        assertFalse(budget.canUseAccurate(1_499L, 150L));
        assertTrue(budget.canUseAccurate(1_500L, 150L));
    }

    @Test public void backwardsClockCannotBypassFrameAgeOrCompletionCooldown() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        assertFalse(budget.canAttempt(999L, 1_000L));
        assertFalse(budget.canAttempt(0L, -1L));
        assertFalse(budget.canUseAccurate(-1L, 500L));
        budget.accurateStarted();
        budget.accurateFinished(2_000L);
        assertFalse(budget.canUseAccurate(1_999L, 500L));
        assertTrue(budget.canUseAccurate(2_500L, 500L));
    }

    @Test public void repeatedSlowRunsNeverPermanentlyDisableOriginalFaceRescue() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        long now = 0L;
        for (int attempt = 0; attempt < 8; attempt++) {
            assertTrue(budget.canUseAccurate(now, 500L));
            budget.accurateStarted();
            // A multi-second inference must not disable later current frames.
            now += 5_000L;
            budget.accurateFinished(now);
            assertFalse(budget.canUseAccurate(now + 499L, 500L));
            now += 500L;
            assertTrue(budget.canUseAccurate(now, 500L));
        }
    }

    @Test public void staleFrameCannotStartRescueAtAgeBoundary() {
        StudyFaceRescueBudget budget = new StudyFaceRescueBudget();
        assertTrue(budget.canAttempt(1_749L, 1_000L));
        assertFalse(budget.canAttempt(1_750L, 1_000L));
        assertFalse(budget.canAttempt(100_000L, 1_000L));
    }
}

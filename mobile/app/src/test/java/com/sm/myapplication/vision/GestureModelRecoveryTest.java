package com.sm.myapplication.vision;

import org.junit.Test;
import static org.junit.Assert.*;

public class GestureModelRecoveryTest {
    @Test public void firstFrameCanInitialize() {
        assertTrue(new GestureModelRecovery().canInitialize(0));
    }

    @Test public void failedInitializationRetriesOnlyAfterDelay() {
        GestureModelRecovery policy = new GestureModelRecovery();
        policy.onInitializationResult(false, 100);
        assertFalse(policy.canInitialize(2_099));
        assertTrue(policy.canInitialize(2_100));
        policy.onInitializationResult(true, 2_100);
        assertTrue(policy.isReady());
        assertFalse(policy.canInitialize(10_000));
        assertEquals(2, policy.getInitializationAttempts());
    }

    @Test public void repeatedInitializationFailuresRemainRetryable() {
        GestureModelRecovery policy = new GestureModelRecovery();
        for (int attempt = 0; attempt < 10; attempt++) {
            long now = attempt * 2_000L;
            assertTrue(policy.canInitialize(now));
            policy.onInitializationResult(false, now);
            assertFalse(policy.canInitialize(now + 1));
        }
        assertTrue(policy.canInitialize(20_000));
    }

    @Test public void threeInferenceFailuresRequestOneModelRebuild() {
        GestureModelRecovery policy = new GestureModelRecovery();
        policy.onInitializationResult(true, 0);
        assertFalse(policy.onInferenceResult(false, 100));
        assertFalse(policy.onInferenceResult(false, 200));
        assertTrue(policy.onInferenceResult(false, 300));
        assertFalse(policy.isReady());
        assertFalse(policy.onInferenceResult(false, 400));
        assertFalse(policy.canInitialize(2_299));
        assertTrue(policy.canInitialize(2_300));
    }

    @Test public void validResultIncludingNoHandsClearsFailureStreak() {
        GestureModelRecovery policy = new GestureModelRecovery();
        policy.onInitializationResult(true, 0);
        for (int frame = 0; frame < 10; frame++) {
            assertFalse(policy.onInferenceResult(false, frame * 300L));
            assertFalse(policy.onInferenceResult(false, frame * 300L + 100));
            // Empty gestures is still a valid MediaPipe result.
            assertFalse(policy.onInferenceResult(true, frame * 300L + 200));
        }
        assertTrue(policy.isReady());
    }

    @Test public void recoveredModelStartsWithCleanFailureCounter() {
        GestureModelRecovery policy = new GestureModelRecovery();
        policy.onInitializationResult(true, 0);
        policy.onInferenceResult(false, 100);
        policy.onInferenceResult(false, 200);
        policy.onInferenceResult(false, 300);
        policy.onInitializationResult(true, 2_300);
        assertFalse(policy.onInferenceResult(false, 2_400));
        assertFalse(policy.onInferenceResult(false, 2_500));
        assertTrue(policy.onInferenceResult(false, 2_600));
    }
}

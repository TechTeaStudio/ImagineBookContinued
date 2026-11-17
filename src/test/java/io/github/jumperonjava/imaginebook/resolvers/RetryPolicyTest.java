package io.github.jumperonjava.imaginebook.resolvers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {
    private static final long SECOND = 1000;

    @Test
    void firstFailureWaitsBeforeRetrying() {
        var policy = new RetryPolicy(3, 30 * SECOND);
        long failedAt = 1_000_000;
        assertFalse(policy.mayRetry(1, failedAt, failedAt + 29 * SECOND));
        assertTrue(policy.mayRetry(1, failedAt, failedAt + 30 * SECOND));
    }

    @Test
    void waitGrowsWithEachFailure() {
        var policy = new RetryPolicy(5, 30 * SECOND);
        assertEquals(30 * SECOND, policy.delayAfter(1));
        assertEquals(60 * SECOND, policy.delayAfter(2));
        assertEquals(90 * SECOND, policy.delayAfter(3));
    }

    @Test
    void givesUpAfterTheLastAttempt() {
        var policy = new RetryPolicy(3, 30 * SECOND);
        long failedAt = 0;
        assertTrue(policy.mayRetry(2, failedAt, failedAt + 10 * 60 * SECOND));
        assertFalse(policy.mayRetry(3, failedAt, failedAt + 10 * 60 * SECOND));
    }

    @Test
    void neverRetriesBeforeAnyFailureWasRecorded() {
        var policy = new RetryPolicy(3, 30 * SECOND);
        assertFalse(policy.mayRetry(0, 0, Long.MAX_VALUE));
    }
}

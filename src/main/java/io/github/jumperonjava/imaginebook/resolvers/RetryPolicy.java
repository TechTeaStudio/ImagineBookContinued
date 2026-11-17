package io.github.jumperonjava.imaginebook.resolvers;

/**
 * When a failed image download may be tried again.
 *
 * <p>Before this, a failed URL was queued again on the very next frame the book was drawn, so a dead
 * link was downloaded over and over for as long as the page stayed open. Now each failure waits
 * {@code baseDelayMillis * failures} and the URL is given up after {@code maxAttempts} failures
 * (until the game restarts).
 */
public record RetryPolicy(int maxAttempts, long baseDelayMillis) {
    public static final RetryPolicy DEFAULT = new RetryPolicy(3, 30_000);

    public long delayAfter(int failures) {
        return baseDelayMillis * failures;
    }

    public boolean mayRetry(int failures, long lastFailureMillis, long nowMillis) {
        if (failures <= 0 || failures >= maxAttempts) {
            return false;
        }
        return nowMillis - lastFailureMillis >= delayAfter(failures);
    }
}

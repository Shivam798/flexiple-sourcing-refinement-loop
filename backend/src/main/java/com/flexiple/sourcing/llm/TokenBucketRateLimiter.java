package com.flexiple.sourcing.llm;

import com.flexiple.sourcing.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A token bucket in front of the model provider, so this application stays under a quota
 * rather than discovering it.
 *
 * <p>The retry policy in {@link GeminiLlmClient} is reactive: it reacts to a 429 that has
 * already happened, which means the recruiter has already waited for a round trip that was
 * never going to succeed. This limiter is the proactive half. A free-tier key allows only a
 * handful of requests per minute, and a single refinement round costs two calls, so a short
 * session reaches the ceiling easily. Pacing outbound calls locally turns a visible error
 * into an invisible pause.
 *
 * <p>Token bucket rather than a fixed window because it permits a burst — the opening search
 * fires two calls back to back and should not be throttled — while still holding the long-run
 * average under the limit. {@code capacity} is the burst the provider tolerates;
 * {@code refillTokens} per {@code refillPeriod} is the sustained rate.
 *
 * <p>Every value is configuration. Moving from a free tier to a paid one, or to a provider
 * with a different ceiling, is an edit to {@code application.yaml} or an environment
 * variable — never a code change.
 */
public class TokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final boolean enabled;
    private final double capacity;
    private final double refillTokens;
    private final long refillPeriodNanos;
    private final Duration maxWait;

    /** Serialises callers, which also gives them the bucket in roughly arrival order. */
    private final ReentrantLock lock = new ReentrantLock(true);

    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(GeminiProperties.RateLimiter properties) {
        this.enabled = properties.enabled();
        this.capacity = Math.max(1, properties.capacity());
        this.refillTokens = Math.max(1, properties.refillTokens());
        this.refillPeriodNanos = Math.max(1, properties.refillPeriod().toNanos());
        this.maxWait = properties.maxWait();
        // Start full, so the first search is never delayed by a limiter that has just booted.
        this.availableTokens = this.capacity;
        this.lastRefillNanos = System.nanoTime();

        if (enabled) {
            log.info("Model rate limiter active: burst {}, sustained {} per {}s, waiting at most {}s",
                    (long) capacity, (long) refillTokens,
                    properties.refillPeriod().toSeconds(), maxWait.toSeconds());
        } else {
            log.info("Model rate limiter disabled");
        }
    }

    /**
     * Blocks until a token is available.
     *
     * @throws LlmRateLimitException when no token arrives within the configured wait, so the
     *     caller surfaces the same countdown the provider's own 429 would have produced
     */
    public void acquire() {
        if (!enabled) {
            return;
        }

        lock.lock();
        try {
            Duration waited = Duration.ZERO;
            while (true) {
                refill();
                if (availableTokens >= 1) {
                    availableTokens -= 1;
                    return;
                }

                Duration untilNextToken = timeUntilNextToken();
                if (waited.plus(untilNextToken).compareTo(maxWait) > 0) {
                    log.warn("Rate limiter could not grant a token within {}s; shedding the request",
                            maxWait.toSeconds());
                    throw new LlmRateLimitException(
                            "Local rate limiter exhausted after waiting " + waited.toSeconds() + "s",
                            untilNextToken, null);
                }

                log.debug("Rate limiter pausing {}ms for a token", untilNextToken.toMillis());
                sleep(untilNextToken);
                waited = waited.plus(untilNextToken);
            }
        } finally {
            lock.unlock();
        }
    }

    /** Continuous refill: tokens accrue smoothly rather than in a lump each period. */
    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        double accrued = (double) elapsed / refillPeriodNanos * refillTokens;
        availableTokens = Math.min(capacity, availableTokens + accrued);
        lastRefillNanos = now;
    }

    private Duration timeUntilNextToken() {
        double shortfall = 1 - availableTokens;
        long nanos = (long) Math.ceil(shortfall / refillTokens * refillPeriodNanos);
        return Duration.ofNanos(Math.max(nanos, Duration.ofMillis(50).toNanos()));
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("Interrupted while waiting for rate limiter", e);
        }
    }

    /** Exposed for tests and for the health endpoint. */
    public double availableTokens() {
        lock.lock();
        try {
            refill();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }
}

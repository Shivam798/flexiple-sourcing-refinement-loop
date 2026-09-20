package com.flexiple.sourcing.llm;

import com.flexiple.sourcing.config.GeminiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * The limiter is pure configuration in, behaviour out — which is exactly what makes it
 * worth testing. These cases pin down that the configured burst, the configured sustained
 * rate and the configured wait ceiling each do what the settings say they do.
 */
class TokenBucketRateLimiterTest {

    @Test
    @DisplayName("the configured burst is granted immediately, so a new search is never throttled")
    void allowsTheConfiguredBurstWithoutWaiting() {
        TokenBucketRateLimiter limiter = limiter(true, 3, 1, Duration.ofSeconds(10), Duration.ofMillis(1));

        long startedAt = System.nanoTime();
        limiter.acquire();
        limiter.acquire();
        limiter.acquire();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(elapsed).isLessThan(Duration.ofMillis(100));
        assertThat(limiter.availableTokens()).isCloseTo(0d, within(0.2d));
    }

    @Test
    @DisplayName("once the burst is spent, a caller waits for the bucket to refill")
    void pacesCallsOnceTheBurstIsSpent() {
        // One token per 200ms: the third call must wait roughly one refill period.
        TokenBucketRateLimiter limiter = limiter(true, 2, 1, Duration.ofMillis(200), Duration.ofSeconds(5));
        limiter.acquire();
        limiter.acquire();

        long startedAt = System.nanoTime();
        limiter.acquire();
        Duration waited = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(waited).isBetween(Duration.ofMillis(100), Duration.ofMillis(600));
    }

    @Test
    @DisplayName("a caller is shed as rate limited rather than blocking past the configured ceiling")
    void shedsCallsThatWouldWaitLongerThanTheCeiling() {
        TokenBucketRateLimiter limiter = limiter(true, 1, 1, Duration.ofMinutes(5), Duration.ofMillis(100));
        limiter.acquire();

        assertThatThrownBy(limiter::acquire)
                .isInstanceOf(LlmRateLimitException.class)
                .hasMessageContaining("rate limiter exhausted");
    }

    @Test
    @DisplayName("disabling the limiter in configuration removes it from the path entirely")
    void disabledLimiterNeverBlocks() {
        TokenBucketRateLimiter limiter = limiter(false, 1, 1, Duration.ofMinutes(10), Duration.ZERO);

        for (int i = 0; i < 50; i++) {
            limiter.acquire();
        }
    }

    private static TokenBucketRateLimiter limiter(
            boolean enabled, int capacity, int refillTokens, Duration refillPeriod, Duration maxWait) {
        return new TokenBucketRateLimiter(
                new GeminiProperties.RateLimiter(enabled, capacity, refillTokens, refillPeriod, maxWait));
    }
}

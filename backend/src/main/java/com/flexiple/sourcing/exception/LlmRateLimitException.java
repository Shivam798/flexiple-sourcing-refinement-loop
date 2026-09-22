package com.flexiple.sourcing.exception;

import java.time.Duration;

/** The provider asked us to slow down. Carries how long to wait, when it tells us. */
public class LlmRateLimitException extends LlmException {

    private final Duration retryAfter;

    public LlmRateLimitException(String technicalMessage, Duration retryAfter, Throwable cause) {
        super(technicalMessage,
                "The model is rate limiting us right now. Your results are still here — try again in a moment.",
                cause);
        this.retryAfter = retryAfter == null ? Duration.ofSeconds(20) : retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}

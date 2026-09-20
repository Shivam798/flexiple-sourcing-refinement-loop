package com.flexiple.sourcing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed configuration for the language model, bound from {@code application.yaml} and
 * overridable by environment variable.
 *
 * <p>The API key is never defaulted to a literal and never logged. It is supplied as
 * {@code GEMINI_API_KEY} and the application refuses to start without it, so a missing key
 * surfaces immediately at boot rather than as a confusing failure on the recruiter's first
 * search.
 *
 * @param apiKey from the {@code GEMINI_API_KEY} environment variable
 * @param model Gemini model id
 * @param baseUrl overridable for testing against a local stub
 * @param timeout per-request ceiling; a recruiter waiting longer than this would rather see an error
 * @param maxProfilesPerScoringCall batch ceiling, protecting both latency and free-tier quota
 * @param thinkingBudget Gemini "thinking" token budget, or null to leave the decision to the
 *     provider. Left unset by default because support varies by model — the lite models
 *     reject the field outright with a 400, and hard-coding it would quietly limit which
 *     models this application can be pointed at
 * @param maxAttempts total tries per call, covering both transport failures and rate limits
 * @param maxRetryWait ceiling on a single wait, so a long provider-advised delay cannot
 *     hold the recruiter's request open indefinitely
 * @param retryWaitBudget ceiling on the total time spent waiting across all retries of one call
 * @param rateLimiter proactive client-side pacing, so the provider's quota is respected
 *     rather than discovered
 */
@ConfigurationProperties(prefix = "sourcing.llm")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        Duration timeout,
        int maxProfilesPerScoringCall,
        Integer thinkingBudget,
        int maxAttempts,
        Duration maxRetryWait,
        Duration retryWaitBudget,
        RateLimiter rateLimiter) {

    /**
     * Token bucket settings. {@code capacity} is the burst the provider tolerates;
     * {@code refillTokens} per {@code refillPeriod} is the sustained rate it allows.
     *
     * <p>Every field is configuration precisely so that moving between a free tier, a paid
     * tier, or another provider is a settings change and never a code change.
     *
     * @param enabled false disables pacing entirely, leaving only the reactive retry
     * @param capacity maximum tokens held, i.e. the largest burst permitted
     * @param refillTokens tokens added per {@code refillPeriod}
     * @param refillPeriod the window {@code refillTokens} refers to
     * @param maxWait how long a call will block for a token before being shed
     */
    public record RateLimiter(
            boolean enabled,
            int capacity,
            int refillTokens,
            Duration refillPeriod,
            Duration maxWait) {

        public RateLimiter {
            capacity = capacity <= 0 ? 5 : capacity;
            refillTokens = refillTokens <= 0 ? 5 : refillTokens;
            refillPeriod = refillPeriod == null ? Duration.ofSeconds(60) : refillPeriod;
            maxWait = maxWait == null ? Duration.ofSeconds(20) : maxWait;
        }

        public static RateLimiter defaults() {
            return new RateLimiter(true, 5, 5, Duration.ofSeconds(60), Duration.ofSeconds(20));
        }
    }

    public GeminiProperties {
        model = (model == null || model.isBlank()) ? "gemini-3.5-flash-lite" : model;
        baseUrl = (baseUrl == null || baseUrl.isBlank())
                ? "https://generativelanguage.googleapis.com/v1beta" : baseUrl;
        timeout = timeout == null ? Duration.ofSeconds(45) : timeout;
        maxProfilesPerScoringCall = maxProfilesPerScoringCall <= 0 ? 20 : maxProfilesPerScoringCall;
        maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        maxRetryWait = maxRetryWait == null ? Duration.ofSeconds(10) : maxRetryWait;
        retryWaitBudget = retryWaitBudget == null ? Duration.ofSeconds(20) : retryWaitBudget;
        rateLimiter = rateLimiter == null ? RateLimiter.defaults() : rateLimiter;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateContentUrl() {
        return "%s/models/%s:generateContent".formatted(baseUrl, model);
    }
}

package com.flexiple.sourcing.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexiple.sourcing.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gemini adapter for {@link LlmClient}.
 *
 * <p>Two deliberate choices here.
 *
 * <p>First, {@code responseSchema} is used so the provider constrains generation to our
 * shape at the source rather than us hoping for it — but the reply is still validated on the
 * way back in ({@link StructuredLlmGateway}), because a schema hint is not a guarantee.
 *
 * <p>Second, retries are bounded and differentiated by cause. A transport failure backs off
 * and tries again. A rate limit waits for exactly as long as the provider asked us to —
 * Gemini returns a {@code retryDelay} in its 429 body — rather than guessing, because a free
 * tier's limit is usually measured in seconds and a two-second wait is invisible to a
 * recruiter where an error is not. Both are capped, per wait and in total, so a call can
 * never sit open longer than the request budget allows; when the budget is spent the failure
 * surfaces and the interface offers a countdown. Nothing is retried blindly, and a malformed
 * response is never retried here at all, since repairing it is a different job that belongs
 * to {@link StructuredLlmGateway}.
 */
@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final RestClient http;
    private final GeminiProperties properties;
    private final ObjectMapper mapper;
    private final FaultInjector faults;
    private final TokenBucketRateLimiter rateLimiter;

    public GeminiLlmClient(
            RestClient geminiRestClient,
            GeminiProperties properties,
            @Qualifier("dataObjectMapper") ObjectMapper mapper,
            FaultInjector faults,
            TokenBucketRateLimiter rateLimiter) {
        this.http = geminiRestClient;
        this.properties = properties;
        this.mapper = mapper;
        this.faults = faults;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasApiKey();
    }

    @Override
    public String generateJson(String prompt, JsonNode responseSchema) {
        if (!isConfigured()) {
            throw new LlmNotConfiguredException();
        }
        Map<String, Object> body = requestBody(prompt, responseSchema);
        int maxAttempts = properties.maxAttempts();
        Duration spentWaiting = Duration.ZERO;
        LlmException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Wait for a token before spending a request. Every retry pays this too, so a
                // retry storm cannot push us further past the quota we are already near.
                rateLimiter.acquire();

                // Checked per attempt, not once per call, so an armed fault is subject to the
                // same retry policy a real one would be — which is the behaviour being shown.
                String injected = applyInjectedFaultIfArmed();
                if (injected != null) {
                    return injected;
                }

                long startedAt = System.currentTimeMillis();
                String raw = http.post()
                        .uri(properties.generateContentUrl())
                        .header("x-goog-api-key", properties.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .exchange((request, response) -> handle(response), false);
                log.info("Gemini call completed in {}ms (attempt {}/{})",
                        System.currentTimeMillis() - startedAt, attempt, maxAttempts);
                return raw;

            } catch (LlmInvalidResponseException e) {
                // Repairing this is the gateway's job, and a bare retry would not fix it.
                throw e;

            } catch (LlmRateLimitException e) {
                lastFailure = e;
                Duration wait = capped(e.retryAfter());
                if (attempt == maxAttempts || spentWaiting.plus(wait).compareTo(properties.retryWaitBudget()) > 0) {
                    log.warn("Rate limited after {} attempt(s) and {}s of waiting; surfacing to the recruiter",
                            attempt, spentWaiting.toSeconds());
                    throw e;
                }
                log.warn("Rate limited on attempt {}/{}; waiting {}s as the provider asked",
                        attempt, maxAttempts, wait.toSeconds());
                sleep(wait);
                spentWaiting = spentWaiting.plus(wait);

            } catch (ResourceAccessException | LlmUnavailableException e) {
                boolean networkDown = LlmUnavailableException.isNetworkFailure(e);
                lastFailure = e instanceof LlmUnavailableException unavailable && !networkDown
                        ? unavailable
                        : networkDown
                                ? LlmUnavailableException.unreachable("Gemini unreachable", e)
                                : new LlmUnavailableException("Gemini transport failure", e);

                Duration wait = backoff(attempt);
                if (attempt == maxAttempts
                        || spentWaiting.plus(wait).compareTo(properties.retryWaitBudget()) > 0) {
                    break;
                }
                log.warn("Gemini transport failure on attempt {}/{} ({}); retrying in {}ms",
                        attempt, maxAttempts, e.toString(), wait.toMillis());
                sleep(wait);
                spentWaiting = spentWaiting.plus(wait);
            }
        }

        throw lastFailure instanceof LlmUnavailableException unavailable
                ? unavailable
                : new LlmUnavailableException("Gemini unavailable after " + maxAttempts + " attempts", lastFailure);
    }

    /** Never wait longer than the configured ceiling, however long the provider asks for. */
    private Duration capped(Duration requested) {
        Duration ceiling = properties.maxRetryWait();
        return requested.compareTo(ceiling) > 0 ? ceiling : requested;
    }

    /**
     * Exponential backoff with jitter for transport failures, which carry no provider guidance.
     *
     * <p>Starts at 700ms and triples, because the failures actually seen here are network
     * blips — a dropped connection, a VPN reconnect, a container losing DNS — and those last
     * seconds. Retrying three times inside one second only guarantees all three land inside
     * the same blip. The jitter keeps concurrent requests from retrying in lockstep, and the
     * caller's wait budget still caps the total.
     */
    private Duration backoff(int attempt) {
        long base = 700L * (long) Math.pow(3, attempt - 1);
        long jittered = base + ThreadLocalRandom.current().nextLong(-base / 5, base / 5 + 1);
        return capped(Duration.ofMillis(Math.max(100L, jittered)));
    }

    /** Extracts the generated text, translating provider failures into our own vocabulary. */
    private String handle(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
        HttpStatusCode status = response.getStatusCode();
        String payload = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        if (status.value() == 429) {
            throw new LlmRateLimitException("Gemini returned 429", retryAfterFrom(response.getHeaders(), payload), null);
        }
        if (status.is5xxServerError()) {
            throw new LlmUnavailableException("Gemini returned " + status.value(), null);
        }
        if (status.value() == 400 || status.value() == 403) {
            // Almost always a bad or unauthorised key; do not echo the payload, it can quote the key.
            throw new LlmException("Gemini rejected the request with " + status.value(),
                    "The model rejected our request. Check that GEMINI_API_KEY is valid and has quota.", null);
        }
        if (!status.is2xxSuccessful()) {
            throw new LlmUnavailableException("Gemini returned unexpected status " + status.value(), null);
        }

        JsonNode root = mapper.readTree(payload);
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text.isMissingNode() || text.asText().isBlank()) {
            String finishReason = root.path("candidates").path(0).path("finishReason").asText("unknown");
            throw new LlmInvalidResponseException("Gemini returned no usable text (finishReason=" + finishReason + ")", null);
        }
        return text.asText();
    }

    private Map<String, Object> requestBody(String prompt, JsonNode responseSchema) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);
        // Low but non-zero: the rubric benefits from some phrasing latitude, the filters do not.
        generationConfig.put("temperature", 0.2);
        // Only sent when explicitly configured: the lite models reject this field with a 400,
        // so sending it unconditionally would silently rule them out.
        if (properties.thinkingBudget() != null) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", properties.thinkingBudget()));
        }

        return Map.of(
                "contents", java.util.List.of(Map.of("parts", java.util.List.of(Map.of("text", prompt)))),
                "generationConfig", generationConfig);
    }

    private Duration retryAfterFrom(HttpHeaders headers, String payload) {
        String header = headers.getFirst("Retry-After");
        if (header != null) {
            try {
                return Duration.ofSeconds(Long.parseLong(header.trim()));
            } catch (NumberFormatException ignored) {
                // Fall through to the body hint below.
            }
        }
        try {
            JsonNode details = mapper.readTree(payload).path("error").path("details");
            for (JsonNode detail : details) {
                String delay = detail.path("retryDelay").asText("");
                if (delay.endsWith("s")) {
                    return Duration.ofSeconds((long) Double.parseDouble(delay.substring(0, delay.length() - 1)));
                }
            }
        } catch (Exception ignored) {
            // A missing hint is not worth failing over; the default below is fine.
        }
        return Duration.ofSeconds(20);
    }

    /**
     * @return corrupted payload text when a malformed response is being simulated, or null
     *     when no fault is armed. Other modes throw, since they have no payload to return.
     */
    private String applyInjectedFaultIfArmed() {
        FaultInjector.Mode mode = faults.consume();
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case RATE_LIMIT -> throw new LlmRateLimitException("Injected rate limit", Duration.ofSeconds(15), null);
            case TIMEOUT -> throw new LlmUnavailableException("Injected timeout", null);
            // Returned, not thrown: the caller must fail to parse this and attempt a repair,
            // which is the path worth demonstrating.
            case MALFORMED -> "Sure! Here are the filters you asked for: {\"required_skills\": [\"AWS RDS\",";
        };
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("Interrupted while waiting to retry", e);
        }
    }
}

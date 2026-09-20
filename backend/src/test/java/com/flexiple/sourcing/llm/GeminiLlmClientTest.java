package com.flexiple.sourcing.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.flexiple.sourcing.config.GeminiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Rate limiting is the failure this application meets most often — free-tier keys enforce
 * a low requests-per-minute ceiling, and one refinement round makes two model calls. The
 * behaviour under a 429 is therefore worth pinning down rather than hoping about.
 *
 * <p>The provider is stubbed here; these tests are about our retry policy, not about Gemini.
 */
class GeminiLlmClientTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent";

    private static final String RATE_LIMIT_BODY = """
            {"error":{"code":429,"message":"Quota exceeded",
              "details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"1s"}]}}""";

    private static final String SUCCESS_BODY = """
            {"candidates":[{"content":{"parts":[{"text":"{\\"ok\\":true}"}]}}]}""";

    @Test
    @DisplayName("a rate limit is waited out and the call succeeds without the recruiter seeing an error")
    void retriesAfterRateLimitAndSucceeds() {
        Fixture fixture = new Fixture(3);
        fixture.server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body(RATE_LIMIT_BODY).contentType(MediaType.APPLICATION_JSON));
        fixture.server.expect(requestTo(URL))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        String result = fixture.client.generateJson("prompt", fixture.schema());

        assertThat(result).isEqualTo("{\"ok\":true}");
        fixture.server.verify();
    }

    @Test
    @DisplayName("a persistent rate limit surfaces after the configured attempts, carrying the provider's own retry delay")
    void surfacesRateLimitOnceAttemptsAreExhausted() {
        Fixture fixture = new Fixture(3);
        for (int i = 0; i < 3; i++) {
            fixture.server.expect(requestTo(URL))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                            .body(RATE_LIMIT_BODY).contentType(MediaType.APPLICATION_JSON));
        }

        assertThatThrownBy(() -> fixture.client.generateJson("prompt", fixture.schema()))
                .isInstanceOf(LlmRateLimitException.class)
                .extracting(e -> ((LlmRateLimitException) e).retryAfter())
                .isEqualTo(Duration.ofSeconds(1));

        fixture.server.verify();
    }

    @Test
    @DisplayName("a transient 503 is retried and recovers")
    void retriesTransientServerErrors() {
        Fixture fixture = new Fixture(3);
        fixture.server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        fixture.server.expect(requestTo(URL))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        assertThat(fixture.client.generateJson("prompt", fixture.schema())).isEqualTo("{\"ok\":true}");
        fixture.server.verify();
    }

    @Test
    @DisplayName("a name-resolution failure is reported as unreachable, not as a slow reply")
    void distinguishesNetworkFailuresFromTimeouts() {
        Throwable dnsFailure = new RuntimeException("wrapper",
                new java.nio.channels.UnresolvedAddressException());

        assertThat(LlmUnavailableException.isNetworkFailure(dnsFailure)).isTrue();
        assertThat(LlmUnavailableException.unreachable("x", dnsFailure).userFacingMessage())
                .contains("Could not reach the model provider");

        assertThat(LlmUnavailableException.isNetworkFailure(new RuntimeException("slow"))).isFalse();
    }

    @Test
    @DisplayName("the API key travels in the header and never in the URL, where it would be logged")
    void sendsKeyAsHeader() {
        Fixture fixture = new Fixture(1);
        fixture.server.expect(requestTo(URL))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        fixture.client.generateJson("prompt", fixture.schema());
        fixture.server.verify();
    }

    /** Wires a client against a stubbed provider, with waits short enough to keep tests fast. */
    private static final class Fixture {
        private final ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        private final MockRestServiceServer server;
        private final GeminiLlmClient client;

        Fixture(int maxAttempts) {
            RestClient.Builder builder = RestClient.builder();
            this.server = MockRestServiceServer.bindTo(builder).build();
            // Pacing is switched off here so these tests measure the retry policy alone.
            GeminiProperties properties = new GeminiProperties(
                    "test-key", "test-model", null, Duration.ofSeconds(5), 20, null,
                    maxAttempts, Duration.ofMillis(50), Duration.ofSeconds(5),
                    new GeminiProperties.RateLimiter(false, 1, 1, Duration.ofSeconds(1), Duration.ZERO));
            this.client = new GeminiLlmClient(builder.build(), properties, mapper, new FaultInjector(),
                    new TokenBucketRateLimiter(properties.rateLimiter()));
        }

        com.fasterxml.jackson.databind.JsonNode schema() {
            return mapper.createObjectNode().put("type", "OBJECT");
        }
    }
}

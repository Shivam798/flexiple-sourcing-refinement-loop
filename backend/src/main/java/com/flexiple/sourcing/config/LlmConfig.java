package com.flexiple.sourcing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.web.client.RestClient;

import com.flexiple.sourcing.llm.TokenBucketRateLimiter;

/**
 * The HTTP client used to reach Gemini.
 *
 * <p>Plain {@link RestClient} rather than a vendor SDK: the call is one POST with a JSON
 * body, and a dedicated SDK would add a dependency and an auth stack to replace roughly
 * thirty lines of HTTP. Timeouts are set explicitly — the default of "wait forever" is
 * never the right behaviour in front of a user.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class LlmConfig {

    /**
     * Paces outbound model calls so a free-tier quota is respected rather than discovered.
     * Constructed from configuration only, so its behaviour is tunable without a rebuild.
     */
    @Bean
    public TokenBucketRateLimiter modelRateLimiter(GeminiProperties properties) {
        return new TokenBucketRateLimiter(properties.rateLimiter());
    }

    @Bean
    public RestClient geminiRestClient(GeminiProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(java.time.Duration.ofSeconds(10))
                .withReadTimeout(properties.timeout());
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        return RestClient.builder().requestFactory(factory).build();
    }
}

package com.flexiple.sourcing.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Two mappers, because this application speaks two JSON dialects.
 *
 * <p>The HTTP API it serves to its own frontend is camelCase, matching TypeScript
 * convention. The data file and every LLM contract are snake_case. Rather than scattering
 * {@code @JsonProperty} across the domain records, the dialect is a property of the mapper:
 * domain records stay clean, and the boundary does the translating.
 *
 * <p>The web mapper is declared explicitly and marked primary. Defining any
 * {@code ObjectMapper} bean makes Spring Boot's own auto-configuration back off, so without
 * this the snake_case mapper below would silently become the one serving HTTP — and the
 * entire API would change dialect.
 */
@Configuration
public class JacksonConfig {

    /** Serves the HTTP API. camelCase, matching the TypeScript client. */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }

    /**
     * Used for {@code profiles.json} and for every LLM request and response.
     *
     * <p>Tolerant in two specific, deliberate ways, both of which turn a discarded response
     * into a usable one: unknown fields are ignored, because models add keys nobody asked for,
     * and implausible integers become null rather than throwing (see
     * {@link LenientIntegerDeserializer}). Neither loosens what we then validate — the domain
     * rules in the services still reject a response that is actually unusable.
     */
    @Bean
    public ObjectMapper dataObjectMapper() {
        SimpleModule lenientNumbers = new SimpleModule()
                .addDeserializer(Integer.class, new LenientIntegerDeserializer());

        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                // Models add fields that were never asked for; that is not a reason to fail a search.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(lenientNumbers)
                .build();
    }
}

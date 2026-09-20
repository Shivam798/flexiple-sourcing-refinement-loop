package com.flexiple.sourcing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for local development only.
 *
 * <p>In Docker Compose the browser never talks to this service directly — the Next.js
 * server proxies to it over the internal network — so this rule exists purely for the
 * {@code npm run dev} + {@code mvn spring-boot:run} workflow.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PATCH", "OPTIONS");
    }
}

package com.flexiple.sourcing.web;

import com.flexiple.sourcing.llm.StructuredLlmGateway;
import com.flexiple.sourcing.repository.ProfileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Readiness, including whether a model key is present.
 *
 * <p>The frontend calls this on load so a missing key produces setup instructions on the
 * first screen rather than an error after the recruiter has typed their requirement.
 */
@RestController
public class HealthController {

    private final StructuredLlmGateway llm;
    private final ProfileRepository profiles;

    public HealthController(StructuredLlmGateway llm, ProfileRepository profiles) {
        this.llm = llm;
        this.profiles = profiles;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "up",
                "profilesLoaded", profiles.findAll().size(),
                "llmConfigured", llm.isConfigured());
    }
}

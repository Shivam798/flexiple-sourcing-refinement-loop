package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Rubric;
import com.flexiple.sourcing.llm.StructuredLlmGateway;
import com.flexiple.sourcing.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Turns a recruiter's sentence into an opening brief (prompt 01).
 *
 * <p>Injects the dataset's real vocabulary into the prompt, which is what stops the model
 * inventing a location or company type that matches nobody, and applies the domain rules
 * the schema cannot express — a rubric needs at least one criterion, and an experience band
 * has to be the right way round.
 */
@Service
public class InterpretService {

    private static final String PROMPT = "01-interpret";

    private final StructuredLlmGateway llm;
    private final ProfileRepository profiles;

    public InterpretService(StructuredLlmGateway llm, ProfileRepository profiles) {
        this.llm = llm;
        this.profiles = profiles;
    }

    public InterpretationResult interpret(String query) {
        InterpretationResult raw = llm.call(
                PROMPT,
                Map.of(
                        "query", query,
                        "locations", String.join(", ", profiles.knownLocations()),
                        "skills", String.join(", ", profiles.knownSkills())),
                InterpretationResult.class,
                InterpretService::validate);

        return new InterpretationResult(
                sanitise(raw.filters()),
                raw.rubric().normalised(),
                raw.assistantReply());
    }

    private static void validate(InterpretationResult result) {
        if (result.filters() == null || result.rubric() == null) {
            throw new IllegalArgumentException("Response must contain both filters and a rubric");
        }
        if (result.rubric().criteria().isEmpty()) {
            throw new IllegalArgumentException("Rubric must contain at least one criterion");
        }
        if (result.rubric().criteria().stream().anyMatch(c -> c.name() == null || c.name().isBlank())) {
            throw new IllegalArgumentException("Every rubric criterion needs a name");
        }
    }

    /** Fixes the one structural mistake worth correcting rather than rejecting. */
    private static Filters sanitise(Filters filters) {
        Integer min = filters.minYearsExperience();
        Integer max = filters.maxYearsExperience();
        if (min != null && max != null && min > max) {
            return filters.withExperienceBand(max, min);
        }
        return filters;
    }

    /** Used by the UI to distinguish "no key configured" from "the call failed". */
    public boolean isConfigured() {
        return llm.isConfigured();
    }

    public Rubric emptyRubric() {
        return Rubric.empty();
    }
}

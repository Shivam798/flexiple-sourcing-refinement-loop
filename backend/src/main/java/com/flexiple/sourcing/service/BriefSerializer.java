package com.flexiple.sourcing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Profile;
import com.flexiple.sourcing.domain.Rubric;
import com.flexiple.sourcing.domain.ScoredProfile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders domain objects into the text handed to the model.
 *
 * <p>Kept separate from the services so prompt payloads are shaped in one place. Profiles
 * are trimmed to the fields a scorer can legitimately cite — sending the full record would
 * cost tokens on data that has no bearing on fit, and every extra field is one more thing
 * a model can hallucinate a citation against.
 */
@Component
public class BriefSerializer {

    private final ObjectMapper mapper;

    public BriefSerializer(@Qualifier("dataObjectMapper") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String filters(Filters filters) {
        return toJson(filters);
    }

    public String rubric(Rubric rubric) {
        return toJson(rubric);
    }

    /** Full candidate records for scoring — every citable field, nothing else. */
    public String profilesForScoring(List<Profile> profiles) {
        return toJson(profiles);
    }

    /**
     * A compact view of what the recruiter is actually looking at, for the refinement call.
     * They referred to these people by position and name, so both must survive.
     */
    public String shownProfiles(List<ScoredProfile> shown) {
        return shown.stream()
                .map(this::describeShown)
                .collect(Collectors.joining("\n"));
    }

    private String describeShown(ScoredProfile scored) {
        Profile p = scored.profile();
        return "- %s | %s | %s | %d yrs | %s | now at %s (%s) | skills: %s | score: %s"
                .formatted(
                        p.id(),
                        p.name(),
                        p.currentTitle(),
                        p.yearsExperience(),
                        p.location(),
                        p.currentCompany(),
                        p.currentCompanyType() == null ? "unknown" : p.currentCompanyType().wireName(),
                        String.join(", ", p.skills()),
                        scored.isScored() ? String.valueOf(scored.score()) : "not scored");
    }

    private String toJson(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // Serialising our own immutable records cannot realistically fail.
            throw new IllegalStateException("Unable to serialise prompt payload", e);
        }
    }
}

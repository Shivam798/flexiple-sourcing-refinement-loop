package com.flexiple.sourcing.controller.dto;

import java.util.List;

/**
 * What the recruiter said about the candidates on screen.
 *
 * @param feedback free-text reaction, e.g. "1 is too junior, 2 and 4 are right"
 * @param verdicts explicit per-card calls from the match / not a match buttons
 */
public record RefineRequest(String feedback, List<Verdict> verdicts) {

    public record Verdict(String profileId, boolean matches) {}

    public RefineRequest {
        verdicts = verdicts == null ? List.of() : List.copyOf(verdicts);
    }

    public boolean isEmpty() {
        return (feedback == null || feedback.isBlank()) && verdicts.isEmpty();
    }
}

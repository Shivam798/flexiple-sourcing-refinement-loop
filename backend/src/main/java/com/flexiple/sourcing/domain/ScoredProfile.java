package com.flexiple.sourcing.domain;

import java.util.List;

/**
 * A candidate together with the LLM's judgement of them against the current rubric.
 *
 * <p>{@code score} and {@code verdict} are nullable on purpose. If the model returns
 * scores for only part of a batch, the missing candidates still passed the objective
 * filters and are still worth showing — they render as "not scored" rather than
 * disappearing. Losing a real match to a partial LLM response would be the worse bug.
 */
public record ScoredProfile(
        Profile profile,
        Integer score,
        Verdict verdict,
        String why,
        List<Evidence> evidence,
        List<String> concerns) {

    public ScoredProfile {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        concerns = concerns == null ? List.of() : List.copyOf(concerns);
    }

    /** Placeholder for a candidate the scoring call did not cover. */
    public static ScoredProfile unscored(Profile profile) {
        return new ScoredProfile(profile, null, null, null, List.of(), List.of());
    }

    public boolean isScored() {
        return score != null;
    }

    /** Unscored candidates sort last; everything else by score descending. */
    public int sortKey() {
        return score == null ? -1 : score;
    }
}

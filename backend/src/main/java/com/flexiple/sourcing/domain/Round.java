package com.flexiple.sourcing.domain;

import java.time.Instant;
import java.util.List;

/**
 * One complete pass of the loop: a brief, what the recruiter said to get here, what
 * changed as a result, and the candidates it produced.
 *
 * <p>Rounds are append-only. Carrying the whole history rather than mutating a single
 * "current" brief is what lets the UI show a per-round diff, and it means a failed
 * refinement can never destroy the results the recruiter is currently looking at.
 */
public record Round(
        int number,
        Filters filters,
        Rubric rubric,
        String recruiterFeedback,
        String assistantReply,
        List<Change> changes,
        List<String> relaxations,
        List<ScoredProfile> results,
        int totalMatched,
        Instant createdAt) {

    public Round {
        changes = changes == null ? List.of() : List.copyOf(changes);
        relaxations = relaxations == null ? List.of() : List.copyOf(relaxations);
        results = results == null ? List.of() : List.copyOf(results);
    }

    /** The opening round, before any results have been fetched. */
    public static Round initial(Filters filters, Rubric rubric, String assistantReply) {
        return new Round(1, filters, rubric, null, assistantReply,
                List.of(), List.of(), List.of(), 0, Instant.now());
    }

    /**
     * Same brief, new results.
     *
     * @param shown the page of candidates the recruiter will actually see
     * @param totalMatched how many passed the filters in total, so the interface can say
     *     "5 of 14" rather than implying the shortlist is the whole result
     */
    public Round withResults(List<ScoredProfile> shown, int totalMatched, List<String> appliedRelaxations) {
        return new Round(number, filters, rubric, recruiterFeedback, assistantReply,
                changes, appliedRelaxations, shown, totalMatched, createdAt);
    }

    /** Same brief, edited by hand in the UI — clears results until the search re-runs. */
    public Round withBrief(Filters newFilters, Rubric newRubric) {
        return new Round(number, newFilters, newRubric, recruiterFeedback, assistantReply,
                changes, List.of(), List.of(), 0, createdAt);
    }
}

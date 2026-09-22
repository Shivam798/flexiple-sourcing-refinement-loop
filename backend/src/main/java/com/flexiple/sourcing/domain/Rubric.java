package com.flexiple.sourcing.domain;

import java.util.List;

/**
 * The subjective half of a search brief: what "good" means for this specific role.
 *
 * <p>Unlike {@link Filters}, this is never executed as code — it is handed to the LLM
 * as the scoring standard. Keeping it structured (rather than a prose paragraph) is what
 * makes it legible to the recruiter, editable field by field, and diffable between
 * refinement rounds.
 */
public record Rubric(String roleSummary, List<Criterion> criteria, List<String> dealbreakers) {

    /** Weights are renormalised, not rejected — see {@link #normalised()}. */
    public record Criterion(String name, double weight, String whatGoodLooksLike, String redFlags) {}

    public Rubric {
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        dealbreakers = dealbreakers == null
                ? List.of()
                : dealbreakers.stream().filter(d -> d != null && !d.isBlank()).toList();
        roleSummary = roleSummary == null ? "" : roleSummary.trim();
    }

    /**
     * Rescales criterion weights to sum to 1.0.
     *
     * <p>Deliberately coercive: a model that returns weights summing to 0.95 has produced
     * a usable rubric with an arithmetic slip, and failing the recruiter's whole search
     * over it would be the wrong trade. Structurally invalid output (no criteria at all)
     * is a different matter and is rejected upstream by the response validator.
     */
    public Rubric normalised() {
        double total = criteria.stream().mapToDouble(Criterion::weight).sum();
        if (criteria.isEmpty() || total <= 0) {
            return this;
        }
        List<Criterion> rescaled = criteria.stream()
                .map(c -> new Criterion(
                        c.name(),
                        Math.round(c.weight() / total * 1000d) / 1000d,
                        c.whatGoodLooksLike(),
                        c.redFlags()))
                .toList();
        return new Rubric(roleSummary, rescaled, dealbreakers);
    }
}

package com.flexiple.sourcing.search;

import com.flexiple.sourcing.domain.Filters;

/**
 * One rung of the relaxation ladder: a single, explainable way to widen a brief that
 * returned too few candidates.
 *
 * <p>Strategy pattern rather than a chain of {@code if} statements, so the ladder is an
 * ordered list that can be reordered or extended without touching the engine, and so each
 * rung owns the sentence the recruiter will read about it.
 */
public interface RelaxationStrategy {

    /** @return false when this rung would change nothing for the given filters. */
    boolean appliesTo(Filters filters);

    Filters relax(Filters filters);

    /** Recruiter-facing description, e.g. "widened experience to 3-8 years". */
    String describe(Filters before, Filters after);

    /**
     * Groups rungs that are successive steps of the same concession.
     *
     * <p>Widening an experience band twice is one concession told twice. Reporting both
     * ("widened to 3-8 years", then "widened to 1-10 years") reads as two separate things
     * happening to the recruiter's search when only the final state is true of it.
     */
    default String concession() {
        return getClass().getName();
    }
}

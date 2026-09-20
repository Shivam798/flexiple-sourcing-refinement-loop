package com.flexiple.sourcing.domain;

/**
 * One field-level edit the refinement step made to the brief, in the recruiter's terms.
 *
 * <p>This is the centrepiece of the loop. A refinement that silently re-runs the search
 * gives the recruiter no reason to trust it; showing "raised minimum experience from 4 to
 * 6 because you said Ananya was too junior" makes the adjustment auditable and, crucially,
 * arguable. The LLM is required to emit one of these per edit it makes.
 *
 * @param target dotted path into the brief, e.g. {@code filters.minYearsExperience}
 * @param from previous value, rendered for display
 * @param to new value, rendered for display
 * @param reason the recruiter-facing justification, tied to what they actually said
 */
public record Change(String target, String from, String to, String reason) {}

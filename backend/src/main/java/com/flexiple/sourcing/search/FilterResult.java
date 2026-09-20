package com.flexiple.sourcing.search;

import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Profile;

import java.util.List;

/**
 * The outcome of applying a brief to the talent pool.
 *
 * @param matches profiles that passed, best-fitting first
 * @param effectiveFilters the filters actually applied, after any relaxation
 * @param relaxations recruiter-facing notes on what was loosened, empty when nothing was
 */
public record FilterResult(List<Profile> matches, Filters effectiveFilters, List<String> relaxations) {

    public FilterResult {
        matches = List.copyOf(matches);
        relaxations = List.copyOf(relaxations);
    }

    public boolean wasRelaxed() {
        return !relaxations.isEmpty();
    }
}

package com.flexiple.sourcing.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The four company backgrounds present in the talent pool.
 *
 * <p>Modelled as an enum rather than a free string so that an LLM cannot widen the
 * vocabulary at runtime: anything outside these four values is rejected at the edge
 * (see {@link #fromWire(String)}) instead of silently producing a filter that matches
 * nobody.
 */
public enum CompanyType {
    STARTUP,
    SCALEUP,
    ENTERPRISE,
    AGENCY;

    /** Lower-case form used by both {@code profiles.json} and the LLM contracts. */
    @JsonValue
    public String wireName() {
        return name().toLowerCase();
    }

    /**
     * @return the matching constant, or {@code null} for an unrecognised value.
     *     Callers drop nulls rather than failing the whole request — one hallucinated
     *     company type should not cost the recruiter their search.
     */
    @JsonCreator
    public static CompanyType fromWire(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (CompanyType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}

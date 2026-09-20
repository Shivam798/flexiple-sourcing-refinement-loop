package com.flexiple.sourcing.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** The LLM's headline call on a candidate, used for the badge on each result card. */
public enum Verdict {
    STRONG,
    POSSIBLE,
    WEAK;

    @JsonValue
    public String wireName() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Verdict fromWire(String value) {
        if (value == null) {
            return POSSIBLE;
        }
        for (Verdict verdict : values()) {
            if (verdict.name().equalsIgnoreCase(value.trim())) {
                return verdict;
            }
        }
        return POSSIBLE;
    }
}

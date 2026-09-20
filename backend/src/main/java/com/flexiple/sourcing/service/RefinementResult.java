package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Change;
import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Rubric;

import java.util.List;

/** What prompt 03 returns: the revised brief, plus the audit trail of what moved and why. */
public record RefinementResult(Filters filters, Rubric rubric, List<Change> changes, String assistantReply) {

    public RefinementResult {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }
}

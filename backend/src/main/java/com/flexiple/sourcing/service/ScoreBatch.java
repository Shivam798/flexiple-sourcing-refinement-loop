package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Evidence;
import com.flexiple.sourcing.domain.Verdict;

import java.util.List;

/** What prompt 02 returns: one entry per candidate the batch covered. */
public record ScoreBatch(List<Entry> scores) {

    public ScoreBatch {
        scores = scores == null ? List.of() : List.copyOf(scores);
    }

    public record Entry(
            String profileId,
            Integer score,
            Verdict verdict,
            String why,
            List<Evidence> evidence,
            List<String> concerns) {

        public Entry {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
            concerns = concerns == null ? List.of() : List.copyOf(concerns);
        }
    }
}

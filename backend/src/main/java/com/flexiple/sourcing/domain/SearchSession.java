package com.flexiple.sourcing.domain;

import com.flexiple.sourcing.exception.SearchFrozenException;

import java.util.ArrayList;
import java.util.List;

/**
 * A single recruiter's search, from the opening sentence through to freeze.
 *
 * <p>Scoped to one session in memory by design — the brief explicitly rules out
 * persistence across sessions, so a durable store would be scope we were asked not to
 * build. See the README for what changes in a real deployment.
 *
 * <p>Mutable by intention: this is the one aggregate in the system that has a lifecycle,
 * and modelling it as a mutable aggregate root guarded by a store is simpler than
 * threading an immutable copy through every service. All state transitions go through
 * the methods below, never through direct field access.
 */
public final class SearchSession {

    private final String id;
    private final String originalQuery;
    private final List<Round> rounds = new ArrayList<>();
    private boolean frozen;

    public SearchSession(String id, String originalQuery, Round firstRound) {
        this.id = id;
        this.originalQuery = originalQuery;
        this.rounds.add(firstRound);
    }

    public String id() {
        return id;
    }

    public String originalQuery() {
        return originalQuery;
    }

    public boolean frozen() {
        return frozen;
    }

    public List<Round> rounds() {
        return List.copyOf(rounds);
    }

    public Round currentRound() {
        return rounds.get(rounds.size() - 1);
    }

    public int roundCount() {
        return rounds.size();
    }

    /** Replaces the latest round in place — used when results or a hand edit arrive. */
    public void replaceCurrentRound(Round round) {
        requireOpen();
        rounds.set(rounds.size() - 1, round);
    }

    /** Opens the next round of the loop after a refinement. */
    public Round openRound(Filters filters, Rubric rubric, String feedback,
                           String assistantReply, List<Change> changes) {
        requireOpen();
        Round round = new Round(rounds.size() + 1, filters, rubric, feedback, assistantReply,
                changes, List.of(), List.of(), 0, java.time.Instant.now());
        rounds.add(round);
        return round;
    }

    public void freeze() {
        this.frozen = true;
    }

    private void requireOpen() {
        if (frozen) {
            throw new SearchFrozenException();
        }
    }
}

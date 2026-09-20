package com.flexiple.sourcing.web.dto;

import com.flexiple.sourcing.domain.Round;
import com.flexiple.sourcing.domain.SearchSession;

import java.util.List;

/**
 * The whole session, returned by every endpoint that changes it.
 *
 * <p>One response shape for the entire loop means the frontend has a single reducer and no
 * chance of the panel and the results drifting out of sync. The full round history travels
 * with it because the interface shows per-round changes, and the payload is small.
 */
public record SessionResponse(
        String id,
        String originalQuery,
        boolean frozen,
        int roundCount,
        List<Round> rounds) {

    public static SessionResponse from(SearchSession session) {
        return new SessionResponse(
                session.id(),
                session.originalQuery(),
                session.frozen(),
                session.roundCount(),
                session.rounds());
    }
}

package com.flexiple.sourcing.store;

import com.flexiple.sourcing.domain.SearchSession;

import java.util.Optional;

/**
 * Where live search sessions are kept.
 *
 * <p>An interface over a one-line map, because this is the seam that changes first when
 * this stops being a single-process exercise: a real deployment puts sessions in Redis with
 * a TTL so any instance can serve any recruiter. Naming the seam costs nothing now and
 * makes that change a new implementation rather than a refactor.
 */
public interface SessionStore {

    void save(SearchSession session);

    Optional<SearchSession> find(String id);
}

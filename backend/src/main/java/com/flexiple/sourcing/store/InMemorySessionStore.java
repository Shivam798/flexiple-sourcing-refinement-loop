package com.flexiple.sourcing.store;

import com.flexiple.sourcing.domain.SearchSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds sessions in process memory for the lifetime of the server.
 *
 * <p>The brief rules out persistence across sessions, so durability here would be building
 * something we were asked not to build. The consequence is deliberate and documented: a
 * restart ends any in-flight search, and running more than one instance would need the
 * Redis-backed implementation described in the README.
 */
@Component
public class InMemorySessionStore implements SessionStore {

    private final Map<String, SearchSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(SearchSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public Optional<SearchSession> find(String id) {
        return Optional.ofNullable(sessions.get(id));
    }
}

package com.flexiple.sourcing.exception;

/** The session id in the URL does not correspond to a live search. */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String id) {
        super("No live search session with id " + id);
    }
}

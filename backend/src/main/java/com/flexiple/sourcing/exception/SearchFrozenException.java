package com.flexiple.sourcing.exception;

/**
 * The search has been frozen and no longer accepts changes.
 *
 * <p>Its own type rather than a bare {@link IllegalStateException}, because the handler has
 * to tell "the recruiter tried to change a frozen search" apart from "something inside the
 * application broke". Those share a JDK exception type but deserve opposite answers: one is
 * a 409 the recruiter caused and can understand, the other is a 500 they can do nothing
 * about, and reporting the second as the first sends them looking in the wrong place.
 */
public class SearchFrozenException extends RuntimeException {

    public SearchFrozenException() {
        super("Search is frozen and can no longer be changed");
    }
}

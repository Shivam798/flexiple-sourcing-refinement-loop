package com.flexiple.sourcing.llm;

/**
 * Base type for every way a language model call can fail.
 *
 * <p>Modelled as distinct subclasses rather than one exception with a status code, because
 * each maps to a different thing the recruiter should see: a rate limit is worth waiting
 * out, a malformed response has already been retried, and a missing key is a setup problem
 * no amount of retrying will fix.
 */
public class LlmException extends RuntimeException {

    private final String userFacingMessage;

    public LlmException(String technicalMessage, String userFacingMessage, Throwable cause) {
        super(technicalMessage, cause);
        this.userFacingMessage = userFacingMessage;
    }

    /** Safe to render in the UI: no stack traces, no provider internals, no key material. */
    public String userFacingMessage() {
        return userFacingMessage;
    }
}

package com.flexiple.sourcing.exception;

/** The model replied, but not with something we can safely apply — repair already attempted. */
public class LlmInvalidResponseException extends LlmException {

    public LlmInvalidResponseException(String technicalMessage, Throwable cause) {
        super(technicalMessage,
                "The model returned a response we could not read. Try rephrasing, or run that step again.",
                cause);
    }
}

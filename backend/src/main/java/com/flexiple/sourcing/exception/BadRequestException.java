package com.flexiple.sourcing.exception;

/** The request cannot be acted on, with a message written for the recruiter to read. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

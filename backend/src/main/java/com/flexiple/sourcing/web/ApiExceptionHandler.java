package com.flexiple.sourcing.web;

import com.flexiple.sourcing.exception.BadRequestException;
import com.flexiple.sourcing.exception.LlmException;
import com.flexiple.sourcing.exception.LlmInvalidResponseException;
import com.flexiple.sourcing.exception.LlmNotConfiguredException;
import com.flexiple.sourcing.exception.LlmRateLimitException;
import com.flexiple.sourcing.exception.LlmUnavailableException;
import com.flexiple.sourcing.exception.SessionNotFoundException;
import com.flexiple.sourcing.web.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every failure into one error shape the frontend can act on.
 *
 * <p>Each kind gets its own code because each deserves a different response in the
 * interface: a rate limit shows a countdown and keeps the current results on screen, a
 * missing key shows setup instructions, and an unreadable model response offers a retry.
 * Collapsing these into a generic 500 would make all of them look like the app is broken.
 *
 * <p>Nothing from a provider payload is echoed back — error bodies can quote the request,
 * and the request carries the API key.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiError> notFound(SessionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("SESSION_NOT_FOUND",
                        "That search has expired. Sessions are held in memory, so a server restart clears them."));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException e) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("SEARCH_FROZEN", e.getMessage()));
    }

    @ExceptionHandler(LlmNotConfiguredException.class)
    public ResponseEntity<ApiError> notConfigured(LlmNotConfiguredException e) {
        log.error("Model call attempted with no API key configured");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of("LLM_NOT_CONFIGURED", e.userFacingMessage()));
    }

    @ExceptionHandler(LlmRateLimitException.class)
    public ResponseEntity<ApiError> rateLimited(LlmRateLimitException e) {
        log.warn("Rate limited by the model provider; advising retry in {}s", e.retryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiError("LLM_RATE_LIMITED", e.userFacingMessage(), (int) e.retryAfter().toSeconds()));
    }

    @ExceptionHandler(LlmInvalidResponseException.class)
    public ResponseEntity<ApiError> invalidResponse(LlmInvalidResponseException e) {
        log.error("Model response unusable after repair attempt", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of("LLM_INVALID_RESPONSE", e.userFacingMessage()));
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ApiError> unavailable(LlmUnavailableException e) {
        log.error("Model provider unavailable", e);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiError.of("LLM_UNAVAILABLE", e.userFacingMessage()));
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ApiError> llmFailure(LlmException e) {
        log.error("Model call failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of("LLM_ERROR", e.userFacingMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unhandled failure", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Something went wrong on our side. Your search is still here."));
    }
}

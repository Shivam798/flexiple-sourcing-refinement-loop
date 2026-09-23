package com.flexiple.sourcing.controller;

import com.flexiple.sourcing.controller.dto.ApiError;
import com.flexiple.sourcing.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates every failure into one error shape the frontend can act on.
 *
 * <p>Each kind gets its own code because each deserves a different response in the
 * interface: a rate limit shows a countdown and keeps the current results on screen, a
 * missing key shows setup instructions, and an unreadable model response offers a retry.
 * Collapsing these into a generic 500 would make all of them look like the app is broken.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so that Spring's own failures — a
 * malformed body, an unknown path, the wrong method — keep their proper 4xx status. An
 * {@code @ExceptionHandler(Exception.class)} in an advice outranks the framework's default
 * resolver, so without this every one of those was answered with a 500, telling the caller
 * the server had broken when in fact the request had.
 *
 * <p>Nothing from a provider payload or an internal exception is echoed back — error bodies
 * can quote the request, and the request carries the API key. Only messages this class or
 * the exception itself wrote for the recruiter are returned.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String INTERNAL_MESSAGE =
            "Something went wrong on our side. Your search is still here.";

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiError> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("SESSION_NOT_FOUND",
                        "That search has expired. Sessions are held in memory, so a server restart clears them."));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException e) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(SearchFrozenException.class)
    public ResponseEntity<ApiError> frozen() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("SEARCH_FROZEN",
                        "This search is frozen, so the brief can no longer change. Start a new search to keep going."));
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
                .body(ApiError.of("INTERNAL_ERROR", INTERNAL_MESSAGE));
    }

    /**
     * The single exit for every failure {@link ResponseEntityExceptionHandler} handles,
     * rewriting its body into the one error shape the rest of this class returns.
     *
     * <p>A client error is logged at warn without a stack trace: a malformed request is the
     * caller's to fix, and recording each one at error with a full trace buries the failures
     * that are actually ours. The exception's own message is never returned — it names
     * parameters, media types and parser offsets that mean nothing to a recruiter.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ApiError error;
        if (statusCode.is4xxClientError()) {
            log.warn("Request rejected with {}: {}", statusCode.value(), ex.getMessage());
            error = ApiError.of("BAD_REQUEST", clientMessage(statusCode));
        } else {
            log.error("Request failed with {}", statusCode.value(), ex);
            error = ApiError.of("INTERNAL_ERROR", INTERNAL_MESSAGE);
        }
        // Headers are carried through: a 405 sets Allow, a 415 sets Accept.
        return new ResponseEntity<>(error, headers, statusCode);
    }

    private static String clientMessage(HttpStatusCode statusCode) {
        if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return "That address does not exist on this server.";
        }
        if (statusCode.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
            return "That request method is not allowed here.";
        }
        if (statusCode.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return "This endpoint accepts JSON. Send Content-Type: application/json.";
        }
        return "That request could not be read. Check it and try again.";
    }
}

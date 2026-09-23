package com.flexiple.sourcing.controller.dto;

/**
 * The single error shape this API returns.
 *
 * @param code stable machine-readable kind, so the UI can choose the right treatment
 * @param message safe to render directly to the recruiter
 * @param retryAfterSeconds present only for rate limits, drives the countdown in the banner
 */
public record ApiError(String code, String message, Integer retryAfterSeconds) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}

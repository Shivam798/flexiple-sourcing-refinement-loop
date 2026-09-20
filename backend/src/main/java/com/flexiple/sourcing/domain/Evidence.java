package com.flexiple.sourcing.domain;

/**
 * One factual citation backing an explanation, e.g. {@code field="skills", value="AWS RDS"}.
 *
 * <p>{@code verified} is set by {@code EvidenceVerifier} after checking the claim against
 * the real profile record. The brief requires explanations to cite actual fields, so we
 * check rather than trust: an unverified citation is stripped from the card instead of
 * being shown to a recruiter who would reasonably believe it.
 */
public record Evidence(String field, String value, boolean verified) {

    public static Evidence unchecked(String field, String value) {
        return new Evidence(field, value, false);
    }

    public Evidence verified(boolean isVerified) {
        return new Evidence(field, value, isVerified);
    }
}

package com.flexiple.sourcing.domain;

import java.util.List;

/**
 * A candidate in the talent pool, mirroring the shape of {@code profiles.json}.
 *
 * <p>Immutable by construction: the compact constructor defensively copies the
 * collections so a profile handed to the scoring layer cannot be mutated underneath
 * the filter layer.
 */
public record Profile(
        String id,
        String name,
        String currentTitle,
        int yearsExperience,
        String location,
        String currentCompany,
        CompanyType currentCompanyType,
        List<String> skills,
        List<PastCompany> pastCompanies,
        String education,
        String summary) {

    public Profile {
        skills = skills == null ? List.of() : List.copyOf(skills);
        pastCompanies = pastCompanies == null ? List.of() : List.copyOf(pastCompanies);
    }

    /** Every company background on the profile, current and past. */
    public List<CompanyType> allCompanyTypes() {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.ofNullable(currentCompanyType),
                        pastCompanies.stream().map(PastCompany::companyType))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}

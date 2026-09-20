package com.flexiple.sourcing.domain;

import java.util.List;

/**
 * The objective, machine-checkable half of a search brief.
 *
 * <p>These are applied by {@code FilterEngine} in plain Java — never by the LLM.
 * Comparing an integer against a range is not a language problem, and keeping it in
 * code buys reproducibility (the same filters always yield the same set), auditability
 * (we can say exactly why a profile was excluded) and a zero-cost re-run when the
 * recruiter edits a value by hand.
 *
 * <p>Every field is "no constraint" when empty or null. The compact constructor
 * normalises nulls away so that no downstream code needs a null check.
 *
 * <p>The {@code withX} methods exist for the relaxation ladder, which widens a filter
 * set step by step when a search returns too few candidates. Records are immutable, so
 * each rung produces a new instance and the original stays intact for the audit trail.
 */
public record Filters(
        List<String> requiredSkills,
        List<String> preferredSkills,
        Integer minYearsExperience,
        Integer maxYearsExperience,
        List<String> locations,
        boolean includeRemote,
        List<CompanyType> currentCompanyTypes,
        List<CompanyType> pastCompanyTypes,
        List<String> titleKeywords,
        List<String> excludeTitles) {

    public Filters {
        requiredSkills = clean(requiredSkills);
        preferredSkills = clean(preferredSkills);
        locations = clean(locations);
        titleKeywords = clean(titleKeywords);
        excludeTitles = clean(excludeTitles);
        currentCompanyTypes = cleanEnums(currentCompanyTypes);
        pastCompanyTypes = cleanEnums(pastCompanyTypes);
    }

    public static Filters empty() {
        return new Filters(
                List.of(), List.of(), null, null, List.of(), false,
                List.of(), List.of(), List.of(), List.of());
    }

    public Filters withExperienceBand(Integer min, Integer max) {
        return new Filters(requiredSkills, preferredSkills, min, max, locations, includeRemote,
                currentCompanyTypes, pastCompanyTypes, titleKeywords, excludeTitles);
    }

    public Filters withoutPreferredSkills() {
        return new Filters(requiredSkills, List.of(), minYearsExperience, maxYearsExperience,
                locations, includeRemote, currentCompanyTypes, pastCompanyTypes, titleKeywords, excludeTitles);
    }

    public Filters withRemoteIncluded() {
        return new Filters(requiredSkills, preferredSkills, minYearsExperience, maxYearsExperience,
                locations, true, currentCompanyTypes, pastCompanyTypes, titleKeywords, excludeTitles);
    }

    public Filters withoutPastCompanyTypes() {
        return new Filters(requiredSkills, preferredSkills, minYearsExperience, maxYearsExperience,
                locations, includeRemote, currentCompanyTypes, List.of(), titleKeywords, excludeTitles);
    }

    public Filters withoutLocations() {
        return new Filters(requiredSkills, preferredSkills, minYearsExperience, maxYearsExperience,
                List.of(), includeRemote, currentCompanyTypes, pastCompanyTypes, titleKeywords, excludeTitles);
    }

    public Filters withoutExperienceBand() {
        return withExperienceBand(null, null);
    }

    public Filters withoutTitleKeywords() {
        return new Filters(requiredSkills, preferredSkills, minYearsExperience, maxYearsExperience,
                locations, includeRemote, currentCompanyTypes, pastCompanyTypes, List.of(), excludeTitles);
    }

    private static List<String> clean(List<String> values) {
        return values == null
                ? List.of()
                : values.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
    }

    private static List<CompanyType> cleanEnums(List<CompanyType> values) {
        return values == null
                ? List.of()
                : values.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }
}

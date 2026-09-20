package com.flexiple.sourcing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexiple.sourcing.domain.Filters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression cover for a failure seen against the live model: asked for a minimum years of
 * experience, it returned a three-hundred-digit number, and the default behaviour discarded
 * an otherwise complete and usable brief.
 */
class LenientIntegerDeserializerTest {

    private final ObjectMapper mapper = new JacksonConfig().dataObjectMapper();

    @Test
    @DisplayName("an implausible number becomes no constraint instead of failing the whole brief")
    void discardsImplausibleNumbersWithoutLosingTheRest() throws Exception {
        String json = """
                {"required_skills":["AWS RDS"],"preferred_skills":[],
                 "min_years_experience":4000000000000000000000000000000000000000000000000,
                 "max_years_experience":7,"locations":["Bangalore"],"include_remote":true,
                 "current_company_types":[],"past_company_types":["startup"],
                 "title_keywords":[],"exclude_titles":[]}""";

        Filters filters = mapper.readValue(json, Filters.class);

        assertThat(filters.minYearsExperience()).isNull();
        // Everything else survives, which is the entire point.
        assertThat(filters.maxYearsExperience()).isEqualTo(7);
        assertThat(filters.requiredSkills()).containsExactly("AWS RDS");
        assertThat(filters.locations()).containsExactly("Bangalore");
        assertThat(filters.includeRemote()).isTrue();
    }

    @Test
    @DisplayName("ordinary values are unaffected")
    void parsesNormalValues() throws Exception {
        String json = """
                {"required_skills":[],"preferred_skills":[],"min_years_experience":4,
                 "max_years_experience":7,"locations":[],"include_remote":false,
                 "current_company_types":[],"past_company_types":[],
                 "title_keywords":[],"exclude_titles":[]}""";

        Filters filters = mapper.readValue(json, Filters.class);

        assertThat(filters.minYearsExperience()).isEqualTo(4);
        assertThat(filters.maxYearsExperience()).isEqualTo(7);
    }

    @Test
    @DisplayName("a number sent as a string is still read, rather than rejected on a technicality")
    void coercesNumericStrings() throws Exception {
        String json = """
                {"required_skills":[],"preferred_skills":[],"min_years_experience":"5",
                 "max_years_experience":null,"locations":[],"include_remote":false,
                 "current_company_types":[],"past_company_types":[],
                 "title_keywords":[],"exclude_titles":[]}""";

        assertThat(mapper.readValue(json, Filters.class).minYearsExperience()).isEqualTo(5);
    }
}

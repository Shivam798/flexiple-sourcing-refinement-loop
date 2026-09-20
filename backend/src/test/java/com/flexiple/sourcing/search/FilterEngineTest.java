package com.flexiple.sourcing.search;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.flexiple.sourcing.domain.CompanyType;
import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Profile;
import com.flexiple.sourcing.repository.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filter engine is the one place in this system where correctness is both critical and
 * silent: a wrong predicate does not throw, it just quietly returns the wrong people, and
 * nobody notices until a recruiter does. That is where the testing time goes.
 *
 * <p>No Spring context — the engine has three collaborators and none of them need a
 * container, so the test stays a fast unit test.
 */
class FilterEngineTest {

    private final FilterEngine engine = newEngine();

    @Test
    @DisplayName("the brief's own example query returns the six intended candidates")
    void findsTheSeededMatchesForTheExampleQuery() {
        // "RDS developers with 4-7 years of experience who have worked at startups,
        //  for a role based in Bangalore."
        Filters filters = new Filters(
                List.of("AWS RDS"), List.of(), 4, 7,
                List.of("Bangalore"), false,
                List.of(), List.of(CompanyType.STARTUP),
                List.of(), List.of());

        List<Profile> matches = engine.match(filters);

        assertThat(matches).extracting(Profile::id)
                .containsExactlyInAnyOrder("p01", "p02", "p03", "p04", "p05", "p06");
    }

    @Test
    @DisplayName("remote-India candidates are only included when the brief allows remote")
    void includesRemoteCandidatesOnlyWhenAsked() {
        Filters onSite = new Filters(
                List.of("AWS RDS"), List.of(), 4, 7, List.of("Bangalore"), false,
                List.of(), List.of(), List.of(), List.of());

        assertThat(engine.match(onSite)).extracting(Profile::location).doesNotContain("Remote - India");
        assertThat(engine.match(onSite.withRemoteIncluded())).extracting(Profile::location)
                .contains("Remote - India");
    }

    @Test
    @DisplayName("skill aliases resolve, so 'RDS' and 'Postgres' find the same people as the canonical names")
    void resolvesSkillAliases() {
        Filters canonical = Filters.empty();
        Filters shorthand = new Filters(
                List.of("rds"), List.of(), null, null, List.of(), false,
                List.of(), List.of(), List.of(), List.of());
        Filters spelledOut = new Filters(
                List.of("AWS RDS"), List.of(), null, null, List.of(), false,
                List.of(), List.of(), List.of(), List.of());

        assertThat(engine.match(shorthand)).isNotEmpty();
        assertThat(engine.match(shorthand)).hasSameSizeAs(engine.match(spelledOut));
        assertThat(engine.match(canonical)).hasSize(48);
    }

    @Test
    @DisplayName("an over-tight brief is relaxed rather than returning nothing, and says what it loosened")
    void relaxesRatherThanReturningAnEmptyScreen() {
        // Deliberately unsatisfiable as written: nobody has 20+ years in this pool.
        Filters tooTight = new Filters(
                List.of("AWS RDS"), List.of("Kubernetes"), 20, 25,
                List.of("Bangalore"), false,
                List.of(), List.of(CompanyType.STARTUP), List.of(), List.of());

        assertThat(engine.match(tooTight)).isEmpty();

        FilterResult relaxed = engine.apply(tooTight, 5);

        assertThat(relaxed.matches()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(relaxed.wasRelaxed()).isTrue();
        assertThat(relaxed.relaxations()).isNotEmpty();
    }

    @Test
    @DisplayName("excluded titles remove the near-miss class without touching the rest")
    void excludesTitlesOnRequest() {
        Filters withDbas = new Filters(
                List.of("AWS RDS"), List.of(), null, null, List.of(), false,
                List.of(), List.of(), List.of(), List.of());
        Filters withoutDbas = new Filters(
                List.of("AWS RDS"), List.of(), null, null, List.of(), false,
                List.of(), List.of(), List.of(), List.of("Database Reliability"));

        assertThat(engine.match(withDbas)).extracting(Profile::currentTitle)
                .contains("Database Reliability Engineer");
        assertThat(engine.match(withoutDbas)).extracting(Profile::currentTitle)
                .doesNotContain("Database Reliability Engineer");
        assertThat(engine.match(withoutDbas).size()).isLessThan(engine.match(withDbas).size());
    }

    @Test
    @DisplayName("repeated steps of one concession are reported once, at their end state")
    void collapsesRepeatedStepsOfTheSameConcession() {
        Filters narrowBand = new Filters(
                List.of("AWS RDS"), List.of(), 6, 6,
                List.of("Bangalore"), false,
                List.of(), List.of(), List.of("Backend Engineer"), List.of());

        FilterResult relaxed = engine.apply(narrowBand, 5);

        assertThat(relaxed.relaxations().stream().filter(note -> note.startsWith("widened experience")))
                .hasSize(1);
    }

    private static FilterEngine newEngine() {
        ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        ProfileRepository repository =
                new ProfileRepository(new ClassPathResource("data/profiles.json"), mapper);
        return new FilterEngine(repository, new SkillNormalizer(), new RelaxationLadder());
    }
}

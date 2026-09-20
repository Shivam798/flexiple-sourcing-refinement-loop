package com.flexiple.sourcing.search;

import com.flexiple.sourcing.domain.CompanyType;
import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Profile;
import com.flexiple.sourcing.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Applies the objective half of a brief to the talent pool. Contains no LLM calls and
 * makes no network requests — given the same filters it returns the same candidates every
 * time, which is what makes the filter panel something a recruiter can trust and edit.
 *
 * <p>Each constraint is expressed as a named {@link Predicate} and the set is composed,
 * so adding a constraint is adding one method rather than extending a conditional.
 */
@Service
public class FilterEngine {

    private static final Logger log = LoggerFactory.getLogger(FilterEngine.class);

    /** Locations treated as reachable by an India-remote candidate. */
    private static final String REMOTE_INDIA = "remote - india";
    private static final List<String> INDIAN_CITIES =
            List.of("bangalore", "bengaluru", "mumbai", "chennai", "delhi ncr", "delhi",
                    "hyderabad", "pune", "kolkata", "ahmedabad");

    private final ProfileRepository profiles;
    private final SkillNormalizer skills;
    private final RelaxationLadder ladder;

    public FilterEngine(ProfileRepository profiles, SkillNormalizer skills, RelaxationLadder ladder) {
        this.profiles = profiles;
        this.skills = skills;
        this.ladder = ladder;
    }

    /**
     * Applies {@code filters}, loosening them one rung at a time until at least
     * {@code minimumResults} candidates survive or the ladder is exhausted.
     *
     * @return the matches plus a recruiter-facing note for every concession made
     */
    public FilterResult apply(Filters filters, int minimumResults) {
        List<Profile> matches = match(filters);
        if (matches.size() >= minimumResults) {
            return new FilterResult(matches, filters, List.of());
        }

        // Rungs compound: widening experience often only pays off once location has also
        // been opened. Every rung that is applied is therefore also reported, so the notes
        // the recruiter reads always describe the filters that were actually used.
        Filters widened = filters;
        // Keyed by concession so successive steps of the same one collapse to their end state.
        Map<String, String> notesByConcession = new LinkedHashMap<>();
        for (RelaxationStrategy rung : ladder.rungs()) {
            if (matches.size() >= minimumResults) {
                break;
            }
            if (!rung.appliesTo(widened)) {
                continue;
            }
            Filters next = rung.relax(widened);
            notesByConcession.put(rung.concession(), rung.describe(widened, next));
            widened = next;
            matches = match(widened);
        }

        List<String> notes = List.copyOf(notesByConcession.values());
        if (!notes.isEmpty()) {
            log.info("Relaxed search to reach {} candidates: {}", matches.size(), notes);
        }
        return new FilterResult(matches, widened, notes);
    }

    /** Strict application with no relaxation — used by tests and by the frozen summary. */
    public List<Profile> match(Filters filters) {
        Predicate<Profile> predicate = experienceIn(filters)
                .and(locatedIn(filters))
                .and(hasAllRequiredSkills(filters))
                .and(hasAnyPreferredSkill(filters))
                .and(currentCompanyMatches(filters))
                .and(anyCompanyMatches(filters))
                .and(titleMatches(filters))
                .and(titleNotExcluded(filters));

        return profiles.findAll().stream()
                .filter(predicate)
                .sorted(bestFitFirst(filters))
                .toList();
    }

    // --- constraints -------------------------------------------------------------

    private Predicate<Profile> experienceIn(Filters f) {
        return p -> (f.minYearsExperience() == null || p.yearsExperience() >= f.minYearsExperience())
                && (f.maxYearsExperience() == null || p.yearsExperience() <= f.maxYearsExperience());
    }

    /**
     * An India-remote candidate satisfies a search for any Indian city when the brief
     * allows remote. Without this the five "Remote - India" profiles silently vanish from
     * every city search, which reads as a bug to a recruiter who knows they exist.
     */
    private Predicate<Profile> locatedIn(Filters f) {
        if (f.locations().isEmpty()) {
            return p -> true;
        }
        List<String> wanted = f.locations().stream().map(String::toLowerCase).toList();
        boolean wantsIndianCity = wanted.stream().anyMatch(INDIAN_CITIES::contains);
        return p -> {
            String actual = p.location().toLowerCase(Locale.ROOT);
            if (wanted.stream().anyMatch(w -> actual.contains(w) || w.contains(actual))) {
                return true;
            }
            return f.includeRemote() && wantsIndianCity && actual.equals(REMOTE_INDIA);
        };
    }

    private Predicate<Profile> hasAllRequiredSkills(Filters f) {
        return p -> f.requiredSkills().stream().allMatch(s -> skills.isSatisfiedBy(s, p.skills()));
    }

    /** Preferred skills are an OR, not an AND — otherwise "nice to have" means "must have". */
    private Predicate<Profile> hasAnyPreferredSkill(Filters f) {
        if (f.preferredSkills().isEmpty()) {
            return p -> true;
        }
        return p -> f.preferredSkills().stream().anyMatch(s -> skills.isSatisfiedBy(s, p.skills()));
    }

    private Predicate<Profile> currentCompanyMatches(Filters f) {
        if (f.currentCompanyTypes().isEmpty()) {
            return p -> true;
        }
        return p -> f.currentCompanyTypes().contains(p.currentCompanyType());
    }

    /**
     * "Has worked at startups" is satisfied by the current role too — a recruiter asking
     * for startup background does not mean "only in the past".
     */
    private Predicate<Profile> anyCompanyMatches(Filters f) {
        if (f.pastCompanyTypes().isEmpty()) {
            return p -> true;
        }
        return p -> {
            List<CompanyType> held = p.allCompanyTypes();
            return f.pastCompanyTypes().stream().anyMatch(held::contains);
        };
    }

    private Predicate<Profile> titleMatches(Filters f) {
        if (f.titleKeywords().isEmpty()) {
            return p -> true;
        }
        return p -> {
            String title = p.currentTitle().toLowerCase(Locale.ROOT);
            return f.titleKeywords().stream().anyMatch(k -> title.contains(k.toLowerCase(Locale.ROOT)));
        };
    }

    private Predicate<Profile> titleNotExcluded(Filters f) {
        if (f.excludeTitles().isEmpty()) {
            return p -> true;
        }
        return p -> {
            String title = p.currentTitle().toLowerCase(Locale.ROOT);
            return f.excludeTitles().stream().noneMatch(x -> title.contains(x.toLowerCase(Locale.ROOT)));
        };
    }

    /**
     * Pre-sorts survivors so that if the scoring call fails we still hand the recruiter a
     * sensible order rather than file order. Preferred-skill overlap first, then seniority.
     */
    private Comparator<Profile> bestFitFirst(Filters f) {
        return Comparator
                .comparingLong((Profile p) -> skills.countSatisfied(f.preferredSkills(), p.skills())).reversed()
                .thenComparing(Comparator.comparingInt(Profile::yearsExperience).reversed())
                .thenComparing(Profile::id);
    }
}

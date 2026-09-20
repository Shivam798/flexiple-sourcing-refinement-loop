package com.flexiple.sourcing.search;

import com.flexiple.sourcing.domain.Filters;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The ordered set of concessions to make when a search returns too few candidates.
 *
 * <p>A sourcing tool that answers "0 results" and stops is a tool nobody opens twice. The
 * ladder loosens the brief one rung at a time, cheapest concession first, and records what
 * it did so the UI can tell the recruiter rather than quietly changing their search.
 *
 * <p>Ordering is a product judgement, not an implementation detail: experience bands are
 * the softest constraint a recruiter holds, location is close to the hardest, so location
 * is given up last.
 */
@Component
public class RelaxationLadder {

    private final List<RelaxationStrategy> rungs = List.of(
            widenExperience(1),
            widenExperience(2),
            dropPreferredSkills(),
            includeRemote(),
            dropPastCompanyTypes(),
            dropTitleKeywords(),
            dropLocations(),
            dropExperienceBand());

    public List<RelaxationStrategy> rungs() {
        return rungs;
    }

    private static RelaxationStrategy widenExperience(int by) {
        return new RelaxationStrategy() {
            @Override
            public boolean appliesTo(Filters f) {
                return f.minYearsExperience() != null || f.maxYearsExperience() != null;
            }

            @Override
            public Filters relax(Filters f) {
                Integer min = f.minYearsExperience() == null ? null : Math.max(0, f.minYearsExperience() - by);
                Integer max = f.maxYearsExperience() == null ? null : f.maxYearsExperience() + by;
                return f.withExperienceBand(min, max);
            }

            @Override
            public String describe(Filters before, Filters after) {
                return "widened experience to %s years".formatted(band(after));
            }

            @Override
            public String concession() {
                return "experience-band";
            }
        };
    }

    private static RelaxationStrategy dropPreferredSkills() {
        return simple(
                f -> !f.preferredSkills().isEmpty(),
                Filters::withoutPreferredSkills,
                "stopped requiring the nice-to-have skills");
    }

    private static RelaxationStrategy includeRemote() {
        return simple(
                f -> !f.locations().isEmpty() && !f.includeRemote(),
                Filters::withRemoteIncluded,
                "included remote candidates");
    }

    private static RelaxationStrategy dropPastCompanyTypes() {
        return simple(
                f -> !f.pastCompanyTypes().isEmpty(),
                Filters::withoutPastCompanyTypes,
                "stopped requiring a specific past company background");
    }

    private static RelaxationStrategy dropTitleKeywords() {
        return simple(
                f -> !f.titleKeywords().isEmpty(),
                Filters::withoutTitleKeywords,
                "stopped matching on job title wording");
    }

    /**
     * Last resort. A brief asking for more experience than anyone in the pool has is better
     * answered with the most senior people available and an explicit note than with an
     * empty screen — the pre-sort already puts the most experienced candidates first.
     */
    private static RelaxationStrategy dropExperienceBand() {
        return simple(
                f -> f.minYearsExperience() != null || f.maxYearsExperience() != null,
                Filters::withoutExperienceBand,
                "removed the experience requirement — nobody in the pool was in that band");
    }

    private static RelaxationStrategy dropLocations() {
        return simple(
                f -> !f.locations().isEmpty(),
                Filters::withoutLocations,
                "opened the search to every location");
    }

    private static RelaxationStrategy simple(
            java.util.function.Predicate<Filters> applies,
            java.util.function.UnaryOperator<Filters> relax,
            String description) {
        return new RelaxationStrategy() {
            @Override
            public boolean appliesTo(Filters f) {
                return applies.test(f);
            }

            @Override
            public Filters relax(Filters f) {
                return relax.apply(f);
            }

            @Override
            public String describe(Filters before, Filters after) {
                return description;
            }
        };
    }

    private static String band(Filters f) {
        Integer min = f.minYearsExperience();
        Integer max = f.maxYearsExperience();
        if (min != null && max != null) {
            return min + "-" + max;
        }
        return min != null ? min + "+" : "up to " + max;
    }
}

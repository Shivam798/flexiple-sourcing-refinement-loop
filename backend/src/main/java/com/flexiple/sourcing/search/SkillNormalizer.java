package com.flexiple.sourcing.search;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reconciles the vocabulary an LLM produces with the vocabulary in the data.
 *
 * <p>Asked for "RDS developers", a model will happily emit {@code "RDS"} while every
 * profile says {@code "AWS RDS"}; ditto "Postgres" against "PostgreSQL". Left alone that
 * mismatch silently empties the result set, which looks like a broken filter rather than
 * a vocabulary problem.
 *
 * <p>Deliberately an alias table plus normalised containment rather than embeddings.
 * Across 45 distinct skills a hand-written table is more precise than cosine similarity,
 * and it is instant, free, and debuggable — you can point at the line that caused a match.
 */
@Component
public class SkillNormalizer {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("rds", "aws rds"),
            Map.entry("amazon rds", "aws rds"),
            Map.entry("postgres", "postgresql"),
            Map.entry("psql", "postgresql"),
            Map.entry("node", "node.js"),
            Map.entry("nodejs", "node.js"),
            Map.entry("ts", "typescript"),
            Map.entry("js", "javascript"),
            Map.entry("k8s", "kubernetes"),
            Map.entry("golang", "go"),
            Map.entry("reactjs", "react"),
            Map.entry("nextjs", "next.js"),
            Map.entry("gcp", "google cloud"),
            Map.entry("ci", "ci/cd"),
            Map.entry("cd", "ci/cd"),
            Map.entry("observability", "monitoring"));

    /** Lower-cases, trims and resolves a known alias to its canonical form. */
    public String canonical(String skill) {
        if (skill == null) {
            return "";
        }
        String cleaned = skill.trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(cleaned, cleaned);
    }

    /**
     * @return true when {@code required} is satisfied by any skill on the profile.
     *     Containment is checked in both directions so that "rds" matches "aws rds" and
     *     "aws rds" matches a profile listing plain "rds".
     */
    public boolean isSatisfiedBy(String required, List<String> profileSkills) {
        String wanted = canonical(required);
        if (wanted.isEmpty()) {
            return true;
        }
        return profileSkills.stream()
                .map(this::canonical)
                .anyMatch(held -> held.equals(wanted) || held.contains(wanted) || wanted.contains(held));
    }

    /** How many of {@code wanted} the profile holds — used to rank near-misses. */
    public long countSatisfied(List<String> wanted, List<String> profileSkills) {
        return wanted.stream().filter(skill -> isSatisfiedBy(skill, profileSkills)).count();
    }
}

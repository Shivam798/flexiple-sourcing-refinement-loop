package com.flexiple.sourcing.service;

import com.flexiple.sourcing.config.GeminiProperties;
import com.flexiple.sourcing.domain.Profile;
import com.flexiple.sourcing.domain.Rubric;
import com.flexiple.sourcing.domain.ScoredProfile;
import com.flexiple.sourcing.llm.StructuredLlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ranks filtered candidates against the rubric (prompt 02).
 *
 * <p>Scores the batch in one call. One call per candidate would multiply the recruiter's
 * wait by the size of the shortlist and burn a free-tier quota that has to survive several
 * refinement rounds in a single session.
 *
 * <p>Candidates the model does not return are kept rather than dropped: they passed the
 * objective filters, which is a fact we established in code, and silently losing a real
 * match to a partial response would be a worse failure than showing one card without a score.
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);
    private static final String PROMPT = "02-score";

    private final StructuredLlmGateway llm;
    private final BriefSerializer serializer;
    private final EvidenceVerifier evidenceVerifier;
    private final GeminiProperties properties;

    public ScoringService(
            StructuredLlmGateway llm,
            BriefSerializer serializer,
            EvidenceVerifier evidenceVerifier,
            GeminiProperties properties) {
        this.llm = llm;
        this.serializer = serializer;
        this.evidenceVerifier = evidenceVerifier;
        this.properties = properties;
    }

    /** @return every candidate given, ranked best first, scored where the model covered them. */
    public List<ScoredProfile> score(List<Profile> candidates, Rubric rubric) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Profile> batch = candidates.stream().limit(properties.maxProfilesPerScoringCall()).toList();
        List<Profile> overflow = candidates.stream().skip(batch.size()).toList();
        if (!overflow.isEmpty()) {
            log.info("Scoring the top {} of {} candidates; {} shown unscored",
                    batch.size(), candidates.size(), overflow.size());
        }

        ScoreBatch scored = llm.call(
                PROMPT,
                Map.of(
                        "rubric", serializer.rubric(rubric),
                        "profiles", serializer.profilesForScoring(batch)),
                ScoreBatch.class,
                ScoringService::validate);

        Map<String, ScoreBatch.Entry> byId = scored.scores().stream()
                .filter(e -> e.profileId() != null)
                .collect(Collectors.toMap(ScoreBatch.Entry::profileId, Function.identity(), (a, b) -> a));

        return java.util.stream.Stream.concat(batch.stream(), overflow.stream())
                .map(profile -> combine(profile, byId.get(profile.id())))
                .sorted(Comparator.comparingInt(ScoredProfile::sortKey).reversed()
                        .thenComparing(s -> s.profile().id()))
                .toList();
    }

    private ScoredProfile combine(Profile profile, ScoreBatch.Entry entry) {
        if (entry == null) {
            return ScoredProfile.unscored(profile);
        }
        return new ScoredProfile(
                profile,
                clamp(entry.score()),
                entry.verdict(),
                entry.why(),
                evidenceVerifier.verify(profile, entry.evidence()),
                entry.concerns());
    }

    private static Integer clamp(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.clamp(score, 0, 100);
    }

    private static void validate(ScoreBatch batch) {
        if (batch.scores().isEmpty()) {
            throw new IllegalArgumentException("Scoring response contained no candidates");
        }
        boolean allIdentified = batch.scores().stream().allMatch(e -> e.profileId() != null && !e.profileId().isBlank());
        if (!allIdentified) {
            throw new IllegalArgumentException("Every score must name the profile it belongs to");
        }
    }
}

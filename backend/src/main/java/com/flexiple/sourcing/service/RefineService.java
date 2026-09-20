package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Round;
import com.flexiple.sourcing.llm.StructuredLlmGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Adjusts the brief from recruiter feedback (prompt 03).
 *
 * <p>Everything the model needs to reason about the feedback is passed explicitly: the
 * current brief, the exact candidates on screen, the recruiter's words, and any per-card
 * verdicts. Feedback like "2 and 4 are right" is positional and means nothing without the
 * list it referred to, so the round carries that list rather than the model being asked to
 * remember it.
 */
@Service
public class RefineService {

    private static final String PROMPT = "03-refine";

    private final StructuredLlmGateway llm;
    private final BriefSerializer serializer;

    public RefineService(StructuredLlmGateway llm, BriefSerializer serializer) {
        this.llm = llm;
        this.serializer = serializer;
    }

    /**
     * @param current the round the recruiter was looking at when they reacted
     * @param feedback their free-text message, possibly empty if they only used the buttons
     * @param verdicts explicit per-card yes/no calls, possibly empty
     */
    public RefinementResult refine(Round current, String feedback, List<ProfileVerdict> verdicts) {
        RefinementResult result = llm.call(
                PROMPT,
                Map.of(
                        "filters", serializer.filters(current.filters()),
                        "rubric", serializer.rubric(current.rubric()),
                        "shown_profiles", serializer.shownProfiles(current.results()),
                        "feedback", feedback == null || feedback.isBlank()
                                ? "(no message — see the explicit verdicts below)" : feedback,
                        "verdicts", renderVerdicts(verdicts)),
                RefinementResult.class,
                RefineService::validate);

        return new RefinementResult(
                result.filters(),
                result.rubric().normalised(),
                result.changes(),
                result.assistantReply());
    }

    private String renderVerdicts(List<ProfileVerdict> verdicts) {
        if (verdicts == null || verdicts.isEmpty()) {
            return "(none given)";
        }
        return verdicts.stream()
                .map(v -> "- %s: %s".formatted(v.profileId(), v.matches() ? "a match" : "not a match"))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static void validate(RefinementResult result) {
        if (result.filters() == null || result.rubric() == null) {
            throw new IllegalArgumentException("Refinement must return a complete brief");
        }
        if (result.rubric().criteria().isEmpty()) {
            throw new IllegalArgumentException("Refinement must not empty the rubric");
        }
        if (result.assistantReply() == null || result.assistantReply().isBlank()) {
            throw new IllegalArgumentException("Refinement must explain itself to the recruiter");
        }
    }

    /** One explicit yes/no the recruiter gave on a card. */
    public record ProfileVerdict(String profileId, boolean matches) {}
}

package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Round;
import com.flexiple.sourcing.domain.Rubric;
import com.flexiple.sourcing.domain.ScoredProfile;
import com.flexiple.sourcing.domain.SearchSession;
import com.flexiple.sourcing.exception.SessionNotFoundException;
import com.flexiple.sourcing.search.FilterEngine;
import com.flexiple.sourcing.search.FilterResult;
import com.flexiple.sourcing.store.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Drives the refinement loop: interpret, search, refine, freeze.
 *
 * <p>The application service the controller talks to. Sequencing lives here rather than in
 * the controller so the HTTP layer stays a thin translation of requests into intentions,
 * and so the loop can be exercised without a servlet.
 *
 * <p>The ordering rule that matters: a round's results are only replaced once new results
 * exist. A failed scoring call leaves the previous round intact, so an error never wipes
 * the candidates the recruiter is reading.
 */
@Service
public class SearchSessionService {

    private static final Logger log = LoggerFactory.getLogger(SearchSessionService.class);

    /**
     * How many candidates a recruiter sees per round.
     *
     * <p>The brief asks for four or five at a time, and that is a product decision rather
     * than a display detail: a page the recruiter can hold in their head is what makes
     * "2 and 4 are right" a sentence they can actually say. It doubles as the target the
     * relaxation ladder widens towards, and as the page the refinement prompt is told about,
     * so the feedback and the candidates it refers to can never drift apart.
     */
    private final int resultsPerRound;

    private final InterpretService interpretService;
    private final ScoringService scoringService;
    private final RefineService refineService;
    private final FilterEngine filterEngine;
    private final SessionStore store;

    public SearchSessionService(
            InterpretService interpretService,
            ScoringService scoringService,
            RefineService refineService,
            FilterEngine filterEngine,
            SessionStore store,
            @Value("${sourcing.results-per-round:5}") int resultsPerRound) {
        this.resultsPerRound = resultsPerRound;
        this.interpretService = interpretService;
        this.scoringService = scoringService;
        this.refineService = refineService;
        this.filterEngine = filterEngine;
        this.store = store;
    }

    /** Step 1: the recruiter's sentence becomes a brief. No search is run yet. */
    public SearchSession start(String query) {
        InterpretationResult interpreted = interpretService.interpret(query);
        SearchSession session = new SearchSession(
                UUID.randomUUID().toString(),
                query,
                Round.initial(interpreted.filters(), interpreted.rubric(), interpreted.assistantReply()));
        store.save(session);
        log.info("Started session {} for query: {}", session.id(), query);
        return session;
    }

    /**
     * Steps 2 and 3: apply the brief in code, then rank the survivors with the model.
     *
     * <p>Split from {@link #start} so the recruiter sees and can start reading the filters
     * and rubric while the slower scoring call is still running, without needing streaming.
     */
    public SearchSession search(String sessionId) {
        SearchSession session = require(sessionId);
        Round current = session.currentRound();

        FilterResult filtered = filterEngine.apply(current.filters(), resultsPerRound);
        log.info("Session {} round {}: {} candidates after filtering{}",
                sessionId, current.number(), filtered.matches().size(),
                filtered.wasRelaxed() ? " (relaxed: " + filtered.relaxations() + ")" : "");

        // Scoring sees a wider pool than the recruiter does, so the page is the best few of
        // many rather than simply the first few the filter happened to return.
        List<ScoredProfile> ranked = scoringService.score(filtered.matches(), current.rubric());
        List<ScoredProfile> shown = ranked.stream().limit(resultsPerRound).toList();

        session.replaceCurrentRound(
                current.withResults(shown, filtered.matches().size(), filtered.relaxations()));
        store.save(session);
        return session;
    }

    /** Step 4: the recruiter reacts, the brief moves, and a new round opens. */
    public SearchSession refine(String sessionId, String feedback, List<RefineService.ProfileVerdict> verdicts) {
        SearchSession session = require(sessionId);
        RefinementResult refined = refineService.refine(session.currentRound(), feedback, verdicts);

        session.openRound(
                refined.filters(), refined.rubric(), feedback, refined.assistantReply(), refined.changes());
        store.save(session);
        log.info("Session {} refined into round {} with {} change(s)",
                sessionId, session.roundCount(), refined.changes().size());
        return search(sessionId);
    }

    /**
     * A hand edit from the filter panel. Deliberately no model call: the recruiter has
     * stated exactly what they want, and asking a model to reinterpret it would be both
     * slower and less faithful.
     */
    public SearchSession editBrief(String sessionId, Filters filters, Rubric rubric) {
        SearchSession session = require(sessionId);
        Round current = session.currentRound();
        session.replaceCurrentRound(current.withBrief(
                filters == null ? current.filters() : filters,
                rubric == null ? current.rubric() : rubric.normalised()));
        store.save(session);
        return search(sessionId);
    }

    /** Step 5: no further changes; the brief and shortlist are final. */
    public SearchSession freeze(String sessionId) {
        SearchSession session = require(sessionId);
        session.freeze();
        store.save(session);
        log.info("Session {} frozen at round {} with {} candidates",
                sessionId, session.roundCount(), session.currentRound().results().size());
        return session;
    }

    public SearchSession get(String sessionId) {
        return require(sessionId);
    }

    private SearchSession require(String sessionId) {
        return store.find(sessionId).orElseThrow(() -> new SessionNotFoundException(sessionId));
    }
}

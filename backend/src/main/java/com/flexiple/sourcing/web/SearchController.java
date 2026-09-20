package com.flexiple.sourcing.web;

import com.flexiple.sourcing.domain.SearchSession;
import com.flexiple.sourcing.service.RefineService;
import com.flexiple.sourcing.service.SearchSessionService;
import com.flexiple.sourcing.web.dto.EditBriefRequest;
import com.flexiple.sourcing.web.dto.RefineRequest;
import com.flexiple.sourcing.web.dto.SessionResponse;
import com.flexiple.sourcing.web.dto.StartSearchRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The refinement loop over HTTP.
 *
 * <p>Thin by design: each method validates the request shape and delegates. All sequencing
 * lives in {@link SearchSessionService}, and every failure is translated centrally by
 * {@link ApiExceptionHandler} rather than being caught here.
 *
 * <p>Interpretation and search are separate endpoints on purpose. The recruiter sees the
 * filters and rubric as soon as the first call returns and can begin reading them while
 * the slower scoring call runs, which buys most of the benefit of streaming for none of
 * the complexity.
 */
@RestController
@RequestMapping("/api/sessions")
public class SearchController {

    private final SearchSessionService sessions;

    public SearchController(SearchSessionService sessions) {
        this.sessions = sessions;
    }

    /** Free text in, brief out. */
    @PostMapping
    public SessionResponse start(@RequestBody StartSearchRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new BadRequestException("Describe who you are looking for to start a search.");
        }
        if (request.query().length() > 2000) {
            throw new BadRequestException("That requirement is too long — keep it under 2000 characters.");
        }
        return respond(sessions.start(request.query().trim()));
    }

    /** Applies the current brief and ranks the survivors. */
    @PostMapping("/{id}/search")
    public SessionResponse search(@PathVariable String id) {
        return respond(sessions.search(id));
    }

    /** Recruiter feedback in, adjusted brief and fresh results out. */
    @PostMapping("/{id}/refine")
    public SessionResponse refine(@PathVariable String id, @RequestBody RefineRequest request) {
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("Tell me what to change — a message, or a verdict on a candidate.");
        }
        return respond(sessions.refine(
                id,
                request.feedback(),
                request.verdicts().stream()
                        .map(v -> new RefineService.ProfileVerdict(v.profileId(), v.matches()))
                        .toList()));
    }

    /** A hand edit from the brief panel. Re-runs the search without a model call. */
    @PatchMapping("/{id}/criteria")
    public SessionResponse editBrief(@PathVariable String id, @RequestBody EditBriefRequest request) {
        if (request == null || (request.filters() == null && request.rubric() == null)) {
            throw new BadRequestException("Nothing to update.");
        }
        return respond(sessions.editBrief(id, request.filters(), request.rubric()));
    }

    /** No further changes; the brief and shortlist are final. */
    @PostMapping("/{id}/freeze")
    public SessionResponse freeze(@PathVariable String id) {
        return respond(sessions.freeze(id));
    }

    @GetMapping("/{id}")
    public SessionResponse get(@PathVariable String id) {
        return respond(sessions.get(id));
    }

    private SessionResponse respond(SearchSession session) {
        return SessionResponse.from(session);
    }
}

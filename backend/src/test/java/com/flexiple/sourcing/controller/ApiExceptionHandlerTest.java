package com.flexiple.sourcing.controller;

import com.flexiple.sourcing.config.GeminiProperties;
import com.flexiple.sourcing.exception.LlmRateLimitException;
import com.flexiple.sourcing.exception.SearchFrozenException;
import com.flexiple.sourcing.exception.SessionNotFoundException;
import com.flexiple.sourcing.service.SearchSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error contract, exercised through the real dispatcher rather than by calling the
 * handler directly — the failures most worth pinning down here are Spring's own, and those
 * only arise when a request actually goes through argument resolution and message parsing.
 *
 * <p>The status codes matter as much as the bodies: the frontend keys its treatment off the
 * code, but anything between it and the server — a proxy, a browser, a log — reads only the
 * status, and a 500 in place of a 400 sends whoever is debugging to the wrong side.
 *
 * <p>Binding {@link GeminiProperties} is not incidental. The application class registers a
 * startup component that depends on it, so every slice test needs it — and binding it here
 * means application.yaml is parsed on every run, which is the only automated check that the
 * placeholder defaults in it still resolve.
 */
@WebMvcTest(controllers = SearchController.class)
@EnableConfigurationProperties(GeminiProperties.class)
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchSessionService sessions;

    @Test
    @DisplayName("an unknown session is a 404")
    void unknownSession() throws Exception {
        given(sessions.get(anyString())).willThrow(new SessionNotFoundException("missing"));

        mvc.perform(get("/api/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    @DisplayName("a frozen search is a 409")
    void frozenSearch() throws Exception {
        given(sessions.freeze(anyString())).willThrow(new SearchFrozenException());

        mvc.perform(post("/api/sessions/abc/freeze"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEARCH_FROZEN"))
                .andExpect(jsonPath("$.message").value(containsString("frozen")));
    }

    /**
     * The regression this class was written for. IllegalStateException used to be mapped to
     * 409 SEARCH_FROZEN wholesale, so an unrelated internal failure — a serialisation error,
     * say — told the recruiter their search was frozen and printed the internal message.
     */
    @Test
    @DisplayName("an unrelated IllegalStateException is a 500, not a frozen search")
    void unrelatedIllegalState() throws Exception {
        given(sessions.get(anyString()))
                .willThrow(new IllegalStateException("Unable to serialise prompt payload"));

        mvc.perform(get("/api/sessions/abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(not(containsString("serialise"))));
    }

    @Test
    @DisplayName("a rate limit carries the retry-after the banner counts down")
    void rateLimit() throws Exception {
        given(sessions.get(anyString()))
                .willThrow(new LlmRateLimitException("429 from provider", Duration.ofSeconds(12), null));

        mvc.perform(get("/api/sessions/abc"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LLM_RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(12));
    }

    @Test
    @DisplayName("a malformed JSON body is a 400, not a 500")
    void malformedBody() throws Exception {
        mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("an unsupported content type is a 415, not a 500")
    void unsupportedMediaType() throws Exception {
        mvc.perform(post("/api/sessions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("the wrong method is a 405, not a 500")
    void methodNotAllowed() throws Exception {
        mvc.perform(delete("/api/sessions/abc"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("an error body never carries an internal exception message")
    void bodyNeverLeaksInternals() throws Exception {
        given(sessions.get(anyString())).willThrow(new RuntimeException("connection string user=admin"));

        mvc.perform(get("/api/sessions/abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(not(containsString("admin"))));
    }
}

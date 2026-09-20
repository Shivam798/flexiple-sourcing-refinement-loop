package com.flexiple.sourcing.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The port through which this application talks to a language model.
 *
 * <p>Narrow on purpose: one method, taking a prompt and the JSON Schema the answer must
 * satisfy, returning raw JSON text. Everything above this line — prompts, validation,
 * repair, batching — is provider-agnostic, so swapping Gemini for another provider is a
 * new implementation of this interface and nothing else.
 */
public interface LlmClient {

    /**
     * @param prompt fully rendered instruction text
     * @param responseSchema JSON Schema constraining the reply
     * @return the model's raw JSON text, not yet parsed or validated
     * @throws LlmException for every failure mode; callers are expected to handle it
     */
    String generateJson(String prompt, JsonNode responseSchema);

    /** Whether a key is configured, so the UI can show setup guidance instead of an error. */
    boolean isConfigured();
}

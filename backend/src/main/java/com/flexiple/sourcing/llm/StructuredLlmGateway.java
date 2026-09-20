package com.flexiple.sourcing.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Turns a prompt plus a schema into a validated domain object, or a well-described failure.
 *
 * <p>This is the single place where "ask the model for structured output" is implemented,
 * so every one of the three model interactions in this application gets the same
 * treatment: render the prompt, constrain generation with the schema, parse, validate
 * against our own rules, and on failure make exactly one repair attempt that feeds the
 * error back to the model.
 *
 * <p>One repair attempt, not a loop. If a model cannot produce valid output when handed
 * the schema, its own output and the specific error, a third attempt is far more likely to
 * burn the recruiter's time and our quota than to succeed.
 */
@Component
public class StructuredLlmGateway {

    private static final Logger log = LoggerFactory.getLogger(StructuredLlmGateway.class);
    private static final String REPAIR_PROMPT = "04-repair";

    private final LlmClient client;
    private final PromptLibrary prompts;
    private final ObjectMapper mapper;

    public StructuredLlmGateway(
            LlmClient client, PromptLibrary prompts, @Qualifier("dataObjectMapper") ObjectMapper mapper) {
        this.client = client;
        this.prompts = prompts;
        this.mapper = mapper;
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    /**
     * @param promptName prompt and schema file name, e.g. {@code 01-interpret}
     * @param variables placeholder values for the prompt template
     * @param type the shape to deserialise into
     * @param validate domain rules the parsed value must satisfy; throws to reject
     * @throws LlmInvalidResponseException when even the repaired response is unusable
     */
    public <T> T call(String promptName, Map<String, String> variables, Class<T> type, Consumer<T> validate) {
        JsonNode schema = prompts.schema(promptName);
        String prompt = prompts.render(promptName, variables);

        String raw = client.generateJson(prompt, schema);
        try {
            return parseAndValidate(raw, type, validate);
        } catch (ResponseRejected first) {
            log.warn("Model response for '{}' rejected ({}); attempting one repair", promptName, first.getMessage());
            String repaired = client.generateJson(repairPrompt(schema, raw, first.getMessage()), schema);
            try {
                T value = parseAndValidate(repaired, type, validate);
                log.info("Repair succeeded for '{}'", promptName);
                return value;
            } catch (ResponseRejected second) {
                throw new LlmInvalidResponseException(
                        "Response for '%s' invalid after repair: %s".formatted(promptName, second.getMessage()), second);
            }
        }
    }

    private <T> T parseAndValidate(String raw, Class<T> type, Consumer<T> validate) throws ResponseRejected {
        try {
            T value = mapper.readValue(stripCodeFence(raw), type);
            validate.accept(value);
            return value;
        } catch (Exception e) {
            throw new ResponseRejected(e.getMessage() == null ? e.toString() : e.getMessage(), e);
        }
    }

    private String repairPrompt(JsonNode schema, String raw, String error) {
        return prompts.render(REPAIR_PROMPT, Map.of(
                "schema", schema.toPrettyString(),
                "error", error,
                "raw", truncate(raw)));
    }

    /**
     * Models occasionally wrap JSON in a Markdown fence despite being asked for raw JSON.
     * Stripping it here costs one method and saves a needless repair round trip.
     */
    private static String stripCodeFence(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        int closing = trimmed.lastIndexOf("```");
        if (firstNewline < 0 || closing <= firstNewline) {
            return trimmed;
        }
        return trimmed.substring(firstNewline + 1, closing).trim();
    }

    private static String truncate(String raw) {
        return raw.length() <= 4000 ? raw : raw.substring(0, 4000) + "\n…truncated…";
    }

    /** Internal signal that a response could not be parsed or failed a domain rule. */
    private static final class ResponseRejected extends Exception {
        ResponseRejected(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

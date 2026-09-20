package com.flexiple.sourcing.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads prompts and their response schemas from the classpath.
 *
 * <p>Prompts live in {@code src/main/resources/prompts} as readable Markdown files rather
 * than as Java string literals. They are as much a part of this system's behaviour as the
 * code is, so they are reviewed, diffed and versioned like code — and a reviewer can read
 * them without opening an IDE.
 *
 * <p>Each prompt is paired with a JSON Schema in {@code prompts/schemas}. That schema is
 * the single source of truth for the shape: it is sent to Gemini as {@code responseSchema}
 * to constrain generation, and the same shape is then re-validated on the way back in.
 */
@Component
public class PromptLibrary {

    private final ObjectMapper mapper;
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> schemaCache = new ConcurrentHashMap<>();

    public PromptLibrary(@Qualifier("dataObjectMapper") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Renders a prompt, substituting {@code {{placeholder}}} tokens.
     *
     * @param name file name without extension, e.g. {@code 01-interpret}
     * @param variables placeholder values; every key must appear in the template
     */
    public String render(String name, Map<String, String> variables) {
        String template = promptCache.computeIfAbsent(name, n -> read("prompts/" + n + ".md"));
        String rendered = template;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }
        return rendered;
    }

    /** The Gemini {@code responseSchema} paired with a prompt of the same name. */
    public JsonNode schema(String name) {
        return schemaCache.computeIfAbsent(name, n -> {
            try {
                return mapper.readTree(read("prompts/schemas/" + n + ".schema.json"));
            } catch (IOException e) {
                throw new UncheckedIOException("Unreadable schema for prompt " + n, e);
            }
        });
    }

    private String read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing classpath resource: " + path, e);
        }
    }
}

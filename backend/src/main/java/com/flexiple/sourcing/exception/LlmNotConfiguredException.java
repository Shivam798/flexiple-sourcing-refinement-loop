package com.flexiple.sourcing.exception;

/** No API key. A setup problem, surfaced distinctly so the UI can show setup instructions. */
public class LlmNotConfiguredException extends LlmException {

    public LlmNotConfiguredException() {
        super("GEMINI_API_KEY is not set",
                "No model API key is configured. Set GEMINI_API_KEY and restart the server.",
                null);
    }
}

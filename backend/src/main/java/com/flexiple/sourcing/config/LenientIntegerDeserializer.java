package com.flexiple.sourcing.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Reads an integer from model output without letting an absurd value fail the whole response.
 *
 * <p>Observed in practice: asked for a minimum years-of-experience, a model returned a number
 * with three hundred digits. Jackson's default behaviour is to throw, which discards an
 * otherwise perfectly good brief — the rubric, every other filter, the change log — over one
 * nonsense field.
 *
 * <p>The judgement here is that a value outside any plausible range carries no information, so
 * the honest representation of it is "no constraint" rather than a failed request. The value
 * is logged so the behaviour is visible rather than silent, and the schema separately asks the
 * provider to stay within bounds — this is the layer that catches it when the provider does not.
 */
public class LenientIntegerDeserializer extends StdDeserializer<Integer> {

    private static final Logger log = LoggerFactory.getLogger(LenientIntegerDeserializer.class);

    /** Nothing this application measures — years, weights as percentages, counts — exceeds this. */
    private static final BigDecimal MAX_PLAUSIBLE = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal MIN_PLAUSIBLE = BigDecimal.valueOf(-1_000_000);

    public LenientIntegerDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != null && parser.currentToken().isNumeric()) {
            BigDecimal value = parser.getDecimalValue();
            if (value.compareTo(MAX_PLAUSIBLE) > 0 || value.compareTo(MIN_PLAUSIBLE) < 0) {
                log.warn("Discarding implausible numeric value from model output: {}",
                        value.toPlainString().length() > 40
                                ? value.toPlainString().substring(0, 40) + "…"
                                : value.toPlainString());
                return null;
            }
            return value.intValue();
        }

        String text = parser.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim()).intValue();
        } catch (NumberFormatException e) {
            log.warn("Discarding unparseable numeric value from model output");
            return null;
        }
    }
}

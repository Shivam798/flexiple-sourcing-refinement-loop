package com.flexiple.sourcing.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Development-only switch that disturbs the next model call, so failure handling can be
 * demonstrated on demand instead of waited for.
 *
 * <p>This is not a mock. The product never fabricates model output: arming a fault only
 * interferes with the response path of a real call, and the switch is exposed solely
 * through {@code /api/dev/fault}, which is registered under the {@code dev} profile.
 *
 * <p>Each mode arms the number of calls needed to exercise its whole path. A malformed
 * response arms twice — the original call and the repair attempt that follows it — so the
 * demonstration ends in the designed error state rather than a repair that accidentally
 * succeeds on garbage input.
 */
@Component
public class FaultInjector {

    private static final Logger log = LoggerFactory.getLogger(FaultInjector.class);

    public enum Mode {
        /**
         * Fail as though the provider returned HTTP 429, for long enough to exhaust the retry
         * policy — otherwise the retry quietly absorbs it and there is nothing to demonstrate.
         */
        RATE_LIMIT(3),
        /** Return unparseable text, exercising parse failure, repair, then clean failure. */
        MALFORMED(2),
        /** Fail as though the transport timed out, once only, so the retry visibly recovers. */
        TIMEOUT(1);

        private final int calls;

        Mode(int calls) {
            this.calls = calls;
        }
    }

    private record Armed(Mode mode, int remaining) {}

    private final AtomicReference<Armed> armed = new AtomicReference<>();

    public void arm(Mode mode) {
        armed.set(new Armed(mode, mode.calls));
        log.warn("Fault injection armed: next {} model call(s) will simulate {}", mode.calls, mode);
    }

    public void disarm() {
        armed.set(null);
    }

    /** Consumes one armed call, disarming when the mode's budget is spent. */
    public Mode consume() {
        Armed current = armed.getAndUpdate(a -> {
            if (a == null) {
                return null;
            }
            return a.remaining() <= 1 ? null : new Armed(a.mode(), a.remaining() - 1);
        });
        return current == null ? null : current.mode();
    }
}

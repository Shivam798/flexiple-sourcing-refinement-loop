package com.flexiple.sourcing.controller;

import com.flexiple.sourcing.exception.BadRequestException;
import com.flexiple.sourcing.llm.FaultInjector;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Arms a simulated model failure, so the recovery paths can be demonstrated on demand.
 *
 * <p>Registered only under the {@code dev} Spring profile, which the production image does
 * not enable. This does not fabricate model output anywhere in the product: arming a fault
 * disturbs the response path of a genuine call, and the loop itself always talks to the
 * real provider.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevController {

    private final FaultInjector faults;

    public DevController(FaultInjector faults) {
        this.faults = faults;
    }

    /** @param mode one of {@code rate_limit}, {@code malformed}, {@code timeout} */
    @PostMapping("/fault")
    public Map<String, Object> arm(@RequestParam String mode) {
        FaultInjector.Mode parsed;
        try {
            parsed = FaultInjector.Mode.valueOf(mode.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown fault mode. Use rate_limit, malformed or timeout.");
        }
        faults.arm(parsed);
        return Map.of("armed", parsed.name().toLowerCase(java.util.Locale.ROOT));
    }

    @DeleteMapping("/fault")
    public Map<String, Object> disarm() {
        faults.disarm();
        return Map.of("armed", false);
    }
}

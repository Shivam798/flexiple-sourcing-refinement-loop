package com.flexiple.sourcing;

import com.flexiple.sourcing.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** The sourcing refinement loop: free text in, a frozen shortlist out. */
@SpringBootApplication
public class SourcingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SourcingApplication.class, args);
    }

    /**
     * Says plainly at startup whether the application can actually call a model.
     *
     * <p>Deliberately a warning rather than a hard failure: the server still serves the
     * frontend's health check, which is what lets the first screen explain the missing key
     * instead of the recruiter discovering it after typing out a requirement.
     */
    @Component
    static class StartupReport {

        private static final Logger log = LoggerFactory.getLogger(StartupReport.class);

        private final GeminiProperties properties;

        StartupReport(GeminiProperties properties) {
            this.properties = properties;
        }

        @EventListener(ApplicationReadyEvent.class)
        void report() {
            if (properties.hasApiKey()) {
                log.info("Model configured: {} — ready to search", properties.model());
            } else {
                log.warn("""
                        
                        ===============================================================
                         GEMINI_API_KEY is not set. Searches will fail until it is.
                         Set it and restart:  export GEMINI_API_KEY=your-key-here
                         Get a free key at:   https://aistudio.google.com/apikey
                        ===============================================================""");
            }
        }
    }
}

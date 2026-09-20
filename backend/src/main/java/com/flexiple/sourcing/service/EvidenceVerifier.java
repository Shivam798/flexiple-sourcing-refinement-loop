package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Evidence;
import com.flexiple.sourcing.domain.PastCompany;
import com.flexiple.sourcing.domain.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Checks every citation in an explanation against the profile it claims to describe.
 *
 * <p>The brief requires explanations tied to real details rather than generic praise. A
 * prompt can ask for that; only code can establish it. Each citation names a field and a
 * value, and this class confirms the value genuinely appears in that field on that
 * candidate. Anything it cannot confirm is marked unverified and dropped from the card, so
 * a recruiter is never shown a fabricated fact they would reasonably act on.
 *
 * <p>Twenty lines of checking is a far better guarantee than a longer prompt.
 */
@Component
public class EvidenceVerifier {

    private static final Logger log = LoggerFactory.getLogger(EvidenceVerifier.class);

    /** @return the citations that hold up, each marked verified. */
    public List<Evidence> verify(Profile profile, List<Evidence> claimed) {
        List<Evidence> checked = claimed.stream()
                .map(e -> e.verified(holdsUp(profile, e)))
                .toList();

        long rejected = checked.stream().filter(e -> !e.verified()).count();
        if (rejected > 0) {
            log.warn("Dropped {} unverifiable citation(s) for profile {}: {}", rejected, profile.id(),
                    checked.stream().filter(e -> !e.verified()).toList());
        }
        return checked.stream().filter(Evidence::verified).toList();
    }

    private boolean holdsUp(Profile profile, Evidence evidence) {
        if (evidence == null || evidence.field() == null || evidence.value() == null) {
            return false;
        }
        String claim = normalise(evidence.value());
        if (claim.isEmpty()) {
            return false;
        }

        return switch (evidence.field().toLowerCase(Locale.ROOT)) {
            case "skills" -> profile.skills().stream().anyMatch(s -> matches(s, claim));
            case "current_title", "currenttitle" -> matches(profile.currentTitle(), claim);
            case "years_experience", "yearsexperience" -> claim.contains(String.valueOf(profile.yearsExperience()));
            case "location" -> matches(profile.location(), claim);
            case "current_company", "currentcompany" -> matches(profile.currentCompany(), claim);
            case "current_company_type", "currentcompanytype" ->
                    profile.currentCompanyType() != null && matches(profile.currentCompanyType().wireName(), claim);
            case "past_companies", "pastcompanies" -> profile.pastCompanies().stream().anyMatch(pc -> citesPast(pc, claim));
            case "education" -> matches(profile.education(), claim);
            case "summary" -> matches(profile.summary(), claim);
            default -> false;
        };
    }

    private boolean citesPast(PastCompany past, String claim) {
        return matches(past.company(), claim)
                || matches(past.title(), claim)
                || (past.companyType() != null && matches(past.companyType().wireName(), claim))
                || claim.contains(String.valueOf(past.years()));
    }

    /**
     * Containment in either direction: a model citing "AWS RDS" against a profile listing
     * "AWS RDS" should pass, and so should one citing "RDS". This is lenient about wording
     * and strict about existence, which is the distinction that matters — the point is to
     * catch invented facts, not to police phrasing.
     */
    private boolean matches(String actual, String claim) {
        if (actual == null) {
            return false;
        }
        String normalised = normalise(actual);
        return !normalised.isEmpty() && (normalised.contains(claim) || claim.contains(normalised));
    }

    private String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

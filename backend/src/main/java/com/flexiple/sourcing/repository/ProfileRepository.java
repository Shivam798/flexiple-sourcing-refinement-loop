package com.flexiple.sourcing.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexiple.sourcing.domain.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The talent pool, loaded once at startup and held in memory.
 *
 * <p>48 profiles is small enough that a database would be pure ceremony, and the brief
 * scopes this to a local file. The interface is deliberately narrow — {@code findAll} and
 * {@code findByIds} — so that swapping this for a real search index later is a change to
 * one class rather than a change to the filter and scoring layers.
 */
@Repository
public class ProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(ProfileRepository.class);

    private final List<Profile> profiles;
    private final Map<String, Profile> byId;

    public ProfileRepository(
            @Value("classpath:data/profiles.json") Resource dataFile,
            @Qualifier("dataObjectMapper") ObjectMapper mapper) {
        this.profiles = load(dataFile, mapper);
        this.byId = profiles.stream().collect(Collectors.toUnmodifiableMap(Profile::id, Function.identity()));
        log.info("Loaded {} profiles into the talent pool", profiles.size());
    }

    public List<Profile> findAll() {
        return profiles;
    }

    public List<Profile> findByIds(List<String> ids) {
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    /** Distinct locations present in the data, injected into the interpretation prompt. */
    public List<String> knownLocations() {
        return profiles.stream().map(Profile::location).distinct().sorted().toList();
    }

    /** Distinct skills present in the data, injected into the interpretation prompt. */
    public List<String> knownSkills() {
        return profiles.stream()
                .flatMap(p -> p.skills().stream())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static List<Profile> load(Resource dataFile, ObjectMapper mapper) {
        try (InputStream in = dataFile.getInputStream()) {
            List<Profile> loaded = mapper.readValue(in, new com.fasterxml.jackson.core.type.TypeReference<List<Profile>>() {});
            if (loaded.isEmpty()) {
                throw new IllegalStateException("profiles.json contained no profiles");
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            // Fail fast at startup: an app that boots without a talent pool has nothing to offer.
            throw new UncheckedIOException("Unable to read data/profiles.json from the classpath", e);
        }
    }
}

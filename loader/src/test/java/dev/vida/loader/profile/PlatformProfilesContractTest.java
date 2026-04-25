/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.profile;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.loader.BootOptions;
import dev.vida.loader.internal.PlatformMorphs;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Контракт для каждого профиля из {@code supported-contract-profiles.txt} (копируется из
 * {@code platform-profiles/supported-contract-profiles.txt}): дескриптор грузится с classpath,
 * перечень платформенных морфов резолвится как {@code .class}.
 */
final class PlatformProfilesContractTest {

    static Stream<String> supportedProfileIds() {
        try {
            return loadSupportedIds().stream();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> loadSupportedIds() throws Exception {
        try (var in = PlatformProfilesContractTest.class.getResourceAsStream(
                "/supported-contract-profiles.txt")) {
            assertThat(in).as("processTestResources must copy supported-contract-profiles.txt").isNotNull();
            List<String> out = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    out.add(line);
                }
            }
            assertThat(out).isNotEmpty();
            return out;
        }
    }

    @ParameterizedTest
    @MethodSource("supportedProfileIds")
    void bundled_profile_loads_and_morph_bundle_on_classpath(String profileId) throws Exception {
        BootOptions opts = BootOptions.builder().platformProfile(profileId).build();
        PlatformProfileLoader.ResolveResult res = PlatformProfileLoader.resolve(opts);
        assertThat(res.failureMessage()).isEmpty();
        assertThat(res.descriptor()).isPresent();
        PlatformProfileDescriptor d = res.descriptor().get();
        ClassLoader cl = PlatformMorphs.class.getClassLoader();
        if (d.morphBundle().isPresent()) {
            for (String fqcn : d.morphBundle().get()) {
                String resource = fqcn.replace('.', '/') + ".class";
                assertThat(cl.getResource(resource))
                        .as("morph %s for profile %s", fqcn, profileId)
                        .isNotNull();
            }
        }
    }
}

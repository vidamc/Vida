/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.loader.BootOptions;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class PlatformProfileLoaderTest {

    @Test
    void parsesBaselineProfileJson() {
        String json = """
                {
                  "profileId": "legacy-121/1.21.1",
                  "gameVersion": "1.21.1",
                  "generation": "LEGACY_121",
                  "mappings": { "strategy": "GAME_DIR_PROGUARD" },
                  "platformBridge": "dev.vida.platform.VanillaBridge"
                }
                """;
        PlatformProfileDescriptor d = PlatformProfileLoader.parseProfileJson(json);
        assertThat(d.profileId()).isEqualTo("legacy-121/1.21.1");
        assertThat(d.gameVersion()).isEqualTo("1.21.1");
        assertThat(d.generation()).isEqualTo(PlatformGeneration.LEGACY_121);
        assertThat(d.mappingsStrategy()).isEqualTo(PlatformMappingsStrategy.GAME_DIR_PROGUARD);
        assertThat(d.platformBridgeClass()).contains("dev.vida.platform.VanillaBridge");
    }

    @Test
    void classpathStrategyRequiresResourcePath() {
        String json = """
                {
                  "profileId": "x/y",
                  "gameVersion": "1.21.1",
                  "generation": "LEGACY_121",
                  "mappings": { "strategy": "CLASSPATH_PROGUARD" }
                }
                """;
        assertThatThrownBy(() -> PlatformProfileLoader.parseProfileJson(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("classpathResource");
    }

    @Test
    void resolves_legacy_1_21_7_descriptor() {
        BootOptions opts = BootOptions.builder().platformProfile("legacy-121/1.21.7").build();
        PlatformProfileLoader.ResolveResult r = PlatformProfileLoader.resolve(opts);
        assertThat(r.failureMessage()).isEmpty();
        assertThat(r.descriptor()).isPresent();
        assertThat(r.descriptor().get().gameVersion()).isEqualTo("1.21.7");
    }

    @Test
    void resolvesFromBootOptionsAndClasspathFixture() throws Exception {
        try (var in = PlatformProfileLoader.class.getResourceAsStream(
                "/META-INF/vida/platform-profiles/legacy-121/1.21.1/profile.json")) {
            assertThat(in).as("processResources must bundle platform profiles").isNotNull();
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            PlatformProfileDescriptor d = PlatformProfileLoader.parseProfileJson(json);
            assertThat(d.profileId()).isEqualTo("legacy-121/1.21.1");
        }

        BootOptions opts = BootOptions.builder()
                .platformProfile("legacy-121/1.21.1")
                .build();
        PlatformProfileLoader.ResolveResult r = PlatformProfileLoader.resolve(opts);
        assertThat(r.failureMessage()).isEmpty();
        assertThat(r.descriptor()).isPresent();
        assertThat(r.descriptor().get().profileId()).isEqualTo("legacy-121/1.21.1");
    }

    @Test
    void rejectsMissingProfileResource() {
        BootOptions opts = BootOptions.builder().platformProfile("legacy-121/9.9.9").build();
        PlatformProfileLoader.ResolveResult r = PlatformProfileLoader.resolve(opts);
        assertThat(r.descriptor()).isEmpty();
        assertThat(r.failureMessage()).isPresent();
    }

    @Test
    void normalizeProfilePathRejectsTraversal() {
        assertThatThrownBy(() -> PlatformProfileLoader.normalizeProfilePath("../evil"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

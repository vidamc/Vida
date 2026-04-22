/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import static org.assertj.core.api.Assertions.*;

import dev.vida.core.Version;
import dev.vida.loader.BootOptions;
import dev.vida.resolver.Provider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Модульные тесты {@link SyntheticProviders}: канонизация версий, источники
 * и приоритет коллизий с реальными модами.
 */
final class SyntheticProvidersTest {

    // ============================================================== canonicalize

    @Test
    void canonicalize_pads_core_parts() {
        assertThat(SyntheticProviders.tryCanonicalize("21"))
                .contains(Version.of(21, 0, 0));
        assertThat(SyntheticProviders.tryCanonicalize("1.21"))
                .contains(Version.of(1, 21, 0));
        assertThat(SyntheticProviders.tryCanonicalize("1.21.1"))
                .contains(Version.parse("1.21.1"));
    }

    @Test
    void canonicalize_preserves_prerelease_and_build() {
        assertThat(SyntheticProviders.tryCanonicalize("1.21-rc1"))
                .contains(Version.parse("1.21.0-rc1"));
        assertThat(SyntheticProviders.tryCanonicalize("21+build.5"))
                .contains(Version.parse("21.0.0+build.5"));
    }

    @Test
    void canonicalize_trims_whitespace() {
        assertThat(SyntheticProviders.tryCanonicalize("  1.21.0  "))
                .contains(Version.parse("1.21.0"));
    }

    @Test
    void canonicalize_rejects_garbage() {
        assertThat(SyntheticProviders.tryCanonicalize(null)).isEmpty();
        assertThat(SyntheticProviders.tryCanonicalize("")).isEmpty();
        assertThat(SyntheticProviders.tryCanonicalize("abc")).isEmpty();
        assertThat(SyntheticProviders.tryCanonicalize("1..2")).isEmpty();
        assertThat(SyntheticProviders.tryCanonicalize("1.2.3.4")).isEmpty();
    }

    // =============================================================== vida

    @Test
    void vida_explicit_option_wins_over_bundled() {
        BootOptions opts = BootOptions.builder()
                .skipDiscovery(true)
                .vidaVersion("9.9.9")
                .build();
        assertThat(SyntheticProviders.resolveVidaVersion(opts))
                .isEqualTo(Version.parse("9.9.9"));
    }

    @Test
    void vida_falls_back_to_bundled_or_fallback() {
        BootOptions opts = BootOptions.builder().skipDiscovery(true).build();
        // bundled ресурс присутствует в :loader:test classpath (стэмпится processResources);
        // если его нет — ожидаем FALLBACK_VIDA_VERSION (0.0.0).
        Version v = SyntheticProviders.resolveVidaVersion(opts);
        assertThat(v).isNotNull();
        // Не должно быть явно "9.9.9" — мы ничего не задавали.
        assertThat(v).isNotEqualTo(Version.parse("9.9.9"));
    }

    // ============================================================ minecraft

    @Test
    void minecraft_provider_skipped_when_no_version() {
        BootOptions opts = BootOptions.builder().skipDiscovery(true).build();
        assertThat(SyntheticProviders.resolveMinecraftVersion(opts)).isEmpty();

        List<Provider> out = SyntheticProviders.build(opts, Set.of());
        assertThat(out).noneMatch(p -> p.id().equals("minecraft"));
    }

    @Test
    void minecraft_provider_uses_explicit_version() {
        BootOptions opts = BootOptions.builder()
                .skipDiscovery(true)
                .minecraftVersion("1.21.1")
                .build();
        Optional<Version> v = SyntheticProviders.resolveMinecraftVersion(opts);
        assertThat(v).contains(Version.parse("1.21.1"));
    }

    @Test
    void extractJsonString_parses_id_field() {
        String json = """
                {"id":"1.21.1","name":"1.21.1","release_target":"1.21.1"}
                """;
        assertThat(SyntheticProviders.extractJsonString(json, "id")).isEqualTo("1.21.1");
        assertThat(SyntheticProviders.extractJsonString(json, "name")).isEqualTo("1.21.1");
        assertThat(SyntheticProviders.extractJsonString(json, "missing")).isNull();
    }

    @Test
    void extractJsonString_returns_null_for_malformed() {
        assertThat(SyntheticProviders.extractJsonString("{}", "id")).isNull();
        assertThat(SyntheticProviders.extractJsonString("", "id")).isNull();
    }

    // ================================================================ java

    @Test
    void java_is_always_present_and_normalized() {
        Version j = SyntheticProviders.resolveJavaVersion();
        assertThat(j).isNotNull();
        assertThat(j.major()).isGreaterThanOrEqualTo(21);
    }

    // =============================================================== build()

    @Test
    void build_produces_vida_and_java_by_default() {
        BootOptions opts = BootOptions.builder().skipDiscovery(true).build();
        List<Provider> out = SyntheticProviders.build(opts, Set.of());
        assertThat(out).extracting(Provider::id)
                .contains("vida", "java")
                .doesNotContain("minecraft");
    }

    @Test
    void build_produces_minecraft_when_version_given() {
        BootOptions opts = BootOptions.builder()
                .skipDiscovery(true)
                .minecraftVersion("1.21.1")
                .build();
        List<Provider> out = SyntheticProviders.build(opts, Set.of());
        assertThat(out).extracting(Provider::id)
                .contains("vida", "java", "minecraft");
        Provider mc = out.stream().filter(p -> p.id().equals("minecraft")).findFirst().orElseThrow();
        assertThat(mc.version()).isEqualTo(Version.parse("1.21.1"));
    }

    @Test
    void real_mod_shadows_synthetic_by_id() {
        BootOptions opts = BootOptions.builder()
                .skipDiscovery(true)
                .minecraftVersion("1.21.1")
                .build();
        List<Provider> out = SyntheticProviders.build(opts, Set.of("vida", "minecraft"));
        assertThat(out).extracting(Provider::id)
                .containsOnly("java");
    }
}

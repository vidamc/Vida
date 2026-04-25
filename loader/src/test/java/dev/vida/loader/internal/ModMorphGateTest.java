/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Version;
import dev.vida.manifest.ModManifest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ModMorphGateTest {

    @Test
    void allows_when_no_active_profile() {
        ModManifest m = mkManifest(Map.of());
        assertThat(ModMorphGate.allowMorphsFromMod(m, Optional.empty())).isTrue();
    }

    @Test
    void allows_when_mod_has_no_declaration() {
        ModManifest m = mkManifest(Map.of());
        assertThat(ModMorphGate.allowMorphsFromMod(m, Optional.of("legacy-121/1.21.1")))
                .isTrue();
    }

    @Test
    void respects_platformProfileIds() {
        ModManifest m = mkManifest(Map.of(
                "vida", Map.of("platformProfileIds", List.of("legacy-121/1.21.7"))));
        assertThat(ModMorphGate.allowMorphsFromMod(m, Optional.of("legacy-121/1.21.7")))
                .isTrue();
        assertThat(ModMorphGate.allowMorphsFromMod(m, Optional.of("legacy-121/1.21.1")))
                .isFalse();
    }

    private static ModManifest mkManifest(Map<String, Object> custom) {
        return ModManifest.builder("demo", Version.parse("1.0.0"), "Demo")
                .custom(custom)
                .build();
    }
}

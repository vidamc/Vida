/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Version;
import dev.vida.manifest.ModManifest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class ModulosInstaladosGlobalTest {

    @AfterEach
    void cleanup() {
        ModulosInstaladosGlobal.resetForTests();
    }

    @Test
    void vista_reflects_installed_list() {
        ModManifest m =
                ModManifest.builder("a", Version.parse("1.0.0"), "A").build();
        ModulosInstaladosGlobal.instalar(List.of(m));
        assertThat(ModulosInstaladosGlobal.vista()).containsExactly(m);
        assertThat(ModulosInstaladosGlobal.buscarPorId("a")).contains(m);
        assertThat(ModulosInstaladosGlobal.buscarPorId("missing")).isEmpty();
    }
}

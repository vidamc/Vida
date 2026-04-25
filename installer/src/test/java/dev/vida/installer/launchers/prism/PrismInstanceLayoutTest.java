/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

final class PrismInstanceLayoutTest {

    @Test
    void layout_points_to_canonical_paths() {
        Path data = Paths.get("/tmp/prism").toAbsolutePath();
        PrismInstanceLayout l = new PrismInstanceLayout(data, "my-vida", "1.21.1", "0.1.0");
        assertThat(l.instanceDir())
                .isEqualTo(data.resolve("instances/my-vida").normalize());
        assertThat(l.instanceCfg().getFileName().toString()).isEqualTo("instance.cfg");
        assertThat(l.mmcPackJson().getFileName().toString()).isEqualTo("mmc-pack.json");
        assertThat(l.componentPatch().getFileName().toString())
                .isEqualTo("dev.vida.loader.json");
        assertThat(l.loaderJar().getFileName().toString())
                .isEqualTo("loader-0.1.0.jar");
        assertThat(l.modsDir().endsWith("minecraft/mods")
                || l.modsDir().endsWith("minecraft\\mods")).isTrue();
    }

    @Test
    void library_maven_coord_builds_from_parts() {
        PrismInstanceLayout l = new PrismInstanceLayout(
                Paths.get("."), "any", "1.21.1", "0.1.0");
        assertThat(l.libraryMavenCoord()).isEqualTo("dev.vida:loader:0.1.0");
    }

    @Test
    void sanitize_removes_unsafe_characters() {
        assertThat(PrismInstanceLayout.sanitizeInstanceName("Vida 1.21.1"))
                .isEqualTo("Vida-1.21.1");
        assertThat(PrismInstanceLayout.sanitizeInstanceName("../evil"))
                .isEqualTo("evil");
        assertThat(PrismInstanceLayout.sanitizeInstanceName(" "))
                .isEqualTo("vida-instance");
        assertThat(PrismInstanceLayout.sanitizeInstanceName(null))
                .isEqualTo("vida-instance");
    }

    @Test
    void default_name_format() {
        assertThat(PrismInstanceLayout.defaultInstanceName("1.21.1", "0.1.0"))
                .isEqualTo("vida-1.21.1-0.1.0");
    }
}

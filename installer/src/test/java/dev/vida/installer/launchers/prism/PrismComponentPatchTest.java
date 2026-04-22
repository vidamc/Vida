/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.mc.JsonTree;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PrismComponentPatchTest {

    private static final Instant TS = Instant.parse("2026-04-01T00:00:00Z");

    @SuppressWarnings("unchecked")
    @Test
    void prism_patch_emits_agents_block() {
        String json = new PrismComponentPatch("1.21.1", "0.1.0", "dev.vida:loader:0.1.0", true)
                .render(TS);
        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(json);

        assertThat(root).containsEntry("uid", "dev.vida.loader")
                .containsEntry("version", "0.1.0")
                .containsEntry("formatVersion", 1L)
                .containsEntry("releaseTime", "2026-04-01T00:00:00Z");

        List<Map<String, Object>> agents = (List<Map<String, Object>>) root.get("+agents");
        assertThat(agents).isNotNull().hasSize(1);
        assertThat(agents.get(0))
                .containsEntry("name", "dev.vida:loader:0.1.0")
                .containsEntry("MMC-hint", "local");

        assertThat(root).doesNotContainKey("+mavenFiles");
    }

    @SuppressWarnings("unchecked")
    @Test
    void multimc_patch_emits_mavenFiles_instead() {
        String json = new PrismComponentPatch("1.21.1", "0.1.0", "dev.vida:loader:0.1.0", false)
                .render(TS);
        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(json);

        assertThat(root).doesNotContainKey("+agents");
        List<Map<String, Object>> libs = (List<Map<String, Object>>) root.get("+mavenFiles");
        assertThat(libs).hasSize(1);
        assertThat(libs.get(0)).containsEntry("MMC-hint", "local");
    }

    @SuppressWarnings("unchecked")
    @Test
    void requires_minecraft_and_jdk21() {
        String json = new PrismComponentPatch("1.21.1", "0.1.0", "g:a:v", true).render(TS);
        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(json);

        List<Map<String, Object>> req = (List<Map<String, Object>>) root.get("requires");
        assertThat(req).hasSize(1);
        assertThat(req.get(0)).containsEntry("uid", "net.minecraft")
                .containsEntry("equals", "1.21.1");

        List<Object> java = (List<Object>) root.get("compatibleJavaMajors");
        assertThat(java).containsExactly(21L);
    }
}

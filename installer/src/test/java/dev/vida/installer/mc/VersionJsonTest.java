/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class VersionJsonTest {

    private static final Path MC = Paths.get("/mc");

    @Test
    @SuppressWarnings("unchecked")
    void contains_standard_minecraft_fields() {
        McLayout layout = new McLayout(MC, "1.21.1", "0.5.0");
        var params = new VersionJson.Params(layout,
                "0123456789abcdef0123456789abcdef01234567", 12345L, Instant.parse("2026-04-21T00:00:00Z"));
        Map<String, Object> m = VersionJson.build(params);

        assertThat(m).containsEntry("id", "vida-1.21.1-0.5.0");
        assertThat(m).containsEntry("inheritsFrom", "1.21.1");
        assertThat(m).containsEntry("type", "release");
        assertThat(m).containsEntry("time", "2026-04-21T00:00:00Z");
        assertThat(m).containsEntry("releaseTime", "2026-04-21T00:00:00Z");
    }

    @Test
    @SuppressWarnings("unchecked")
    void arguments_jvm_has_javaagent_and_vida_props() {
        McLayout layout = new McLayout(MC, "1.21.1", "0.5.0");
        var params = new VersionJson.Params(layout,
                "0".repeat(40), 1L, Instant.EPOCH);
        Map<String, Object> tree = VersionJson.build(params);

        List<Object> jvm = (List<Object>)
                ((Map<String, Object>) tree.get("arguments")).get("jvm");
        assertThat(jvm)
                .anyMatch(s -> String.valueOf(s)
                        .equals("-javaagent:${library_directory}/dev/vida/vida-loader/0.5.0/vida-loader-0.5.0.jar"))
                .anyMatch(s -> String.valueOf(s).equals("-Dvida.mods=${game_directory}/mods"))
                .anyMatch(s -> String.valueOf(s).equals("-Dvida.config=${game_directory}/vida/config"))
                .anyMatch(s -> String.valueOf(s).equals("-Dvida.loader.version=0.5.0"))
                .anyMatch(s -> String.valueOf(s).equals("-Dvida.minecraft.version=1.21.1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void libraries_entry_is_well_formed() {
        McLayout layout = new McLayout(MC, "1.21.1", "0.5.0");
        String sha = "0123456789abcdef0123456789abcdef01234567";
        var params = new VersionJson.Params(layout, sha, 40717L, Instant.EPOCH);

        Map<String, Object> tree = VersionJson.build(params);
        List<Object> libs = (List<Object>) tree.get("libraries");
        assertThat(libs).hasSize(1);
        Map<String, Object> lib = (Map<String, Object>) libs.get(0);
        assertThat(lib).containsEntry("name", "dev.vida:vida-loader:0.5.0");

        Map<String, Object> dl = (Map<String, Object>) lib.get("downloads");
        Map<String, Object> art = (Map<String, Object>) dl.get("artifact");
        assertThat(art).containsEntry("path", "dev/vida/vida-loader/0.5.0/vida-loader-0.5.0.jar");
        assertThat(art).containsEntry("sha1", sha);
        assertThat(art).containsEntry("size", 40717L);
        assertThat(art).containsEntry("url", "");
    }

    @Test
    void render_produces_valid_json() {
        McLayout layout = new McLayout(MC, "1.21.1", "0.5.0");
        String json = VersionJson.render(new VersionJson.Params(
                layout, "0".repeat(40), 100L, Instant.EPOCH));
        // Round-trip через JsonTree должен отработать без ошибок.
        Object parsed = JsonTree.parse(json);
        assertThat(parsed).isInstanceOf(Map.class);
    }

    @Test
    void rejects_negative_size() {
        McLayout layout = new McLayout(MC, "1.21.1", "0.5.0");
        assertThatThrownBy(() ->
                new VersionJson.Params(layout, "0".repeat(40), -1L, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

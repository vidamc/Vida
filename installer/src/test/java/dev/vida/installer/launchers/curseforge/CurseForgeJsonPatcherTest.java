/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.curseforge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CurseForgeJsonPatcherTest {

    @TempDir Path tmp;

    private Path writeInstance(Map<String, Object> data) throws IOException {
        Path js = tmp.resolve("minecraftinstance.json");
        Files.writeString(js, JsonTree.write(data), StandardCharsets.UTF_8);
        return js;
    }

    @Test
    void adds_javaArgsOverride_when_absent() throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "TestInstance");
        data.put("gameVersion", "1.21.1");
        Path js = writeInstance(data);

        CurseForgeJsonPatcher.Result r = CurseForgeJsonPatcher.patch(js, "/path/to/loader.jar");

        assertThat(r.alreadyAgent()).isFalse();
        assertThat(r.previousArgs()).isNull();

        String patched = Files.readString(js, StandardCharsets.UTF_8);
        assertThat(patched).contains("-javaagent:/path/to/loader.jar");
    }

    @Test
    void appends_to_existing_javaArgsOverride() throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "TestInstance");
        data.put("javaArgsOverride", "-Xmx4G -Xms2G");
        Path js = writeInstance(data);

        CurseForgeJsonPatcher.Result r = CurseForgeJsonPatcher.patch(js, "/path/loader.jar");

        assertThat(r.alreadyAgent()).isFalse();
        assertThat(r.previousArgs()).isEqualTo("-Xmx4G -Xms2G");

        String patched = Files.readString(js, StandardCharsets.UTF_8);
        assertThat(patched).contains("-Xmx4G -Xms2G -javaagent:/path/loader.jar");
    }

    @Test
    void replaces_existing_javaagent() throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "TestInstance");
        data.put("javaArgsOverride", "-Xmx4G -javaagent:/old/agent.jar -Dfoo=bar");
        Path js = writeInstance(data);

        CurseForgeJsonPatcher.Result r = CurseForgeJsonPatcher.patch(js, "/new/loader.jar");

        assertThat(r.alreadyAgent()).isTrue();

        String patched = Files.readString(js, StandardCharsets.UTF_8);
        assertThat(patched).contains("-javaagent:/new/loader.jar");
        assertThat(patched).doesNotContain("/old/agent.jar");
        assertThat(patched).contains("-Xmx4G");
        assertThat(patched).contains("-Dfoo=bar");
    }

    @Test
    void handles_empty_javaArgsOverride() throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "TestInstance");
        data.put("javaArgsOverride", "");
        Path js = writeInstance(data);

        CurseForgeJsonPatcher.Result r = CurseForgeJsonPatcher.patch(js, "/p/loader.jar");
        assertThat(r.alreadyAgent()).isFalse();

        String patched = Files.readString(js, StandardCharsets.UTF_8);
        assertThat(patched).contains("-javaagent:/p/loader.jar");
    }
}

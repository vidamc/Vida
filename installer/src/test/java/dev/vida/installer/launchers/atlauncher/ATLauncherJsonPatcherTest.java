/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.atlauncher;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.mc.JsonTree;
import dev.vida.installer.mc.VidaInstallerJvm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ATLauncherJsonPatcherTest {

    private static final String MV = "1.21.1";
    private static final String LV = "0.5.0";

    private static String vidaSuffix() {
        return VidaInstallerJvm.spaceSeparatedInstallerJvmProps(MV, LV);
    }

    @SuppressWarnings("unchecked")
    @Test
    void patches_empty_launcher_block(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("instance.json");
        Files.writeString(f, """
                {"id":"1.21.1","launcher":{"name":"Foo"}}
                """, StandardCharsets.UTF_8);

        var r = ATLauncherJsonPatcher.patch(f, "/tmp/vida/v.jar", MV, LV);

        assertThat(r.alreadyAgent()).isFalse();
        assertThat(r.newArgs()).isEqualTo("-javaagent:/tmp/vida/v.jar " + vidaSuffix());
        assertThat(r.previousArgs()).isNull();

        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(
                Files.readString(f, StandardCharsets.UTF_8));
        Map<String, Object> launcher = (Map<String, Object>) root.get("launcher");
        assertThat(launcher).containsEntry("javaArguments", "-javaagent:/tmp/vida/v.jar " + vidaSuffix());
    }

    @SuppressWarnings("unchecked")
    @Test
    void preserves_existing_non_agent_args(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("instance.json");
        Files.writeString(f, """
                {"id":"1.21.1","launcher":{"javaArguments":"-Xmx4G -XX:+UseG1GC"}}
                """, StandardCharsets.UTF_8);

        var r = ATLauncherJsonPatcher.patch(f, "/opt/vida/loader.jar", MV, LV);

        assertThat(r.newArgs())
                .isEqualTo("-Xmx4G -XX:+UseG1GC -javaagent:/opt/vida/loader.jar " + vidaSuffix());
    }

    @Test
    void replaces_previous_vida_agent_not_other_agent(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("instance.json");
        // Старый Vida-agent + чужой agent.
        Files.writeString(f, """
                {"id":"1.21.1","launcher":{"javaArguments":
                "-Xmx4G -javaagent:/old/vida/vida-loader-0.0.9.jar -javaagent:/opt/jrebel.jar"}}
                """, StandardCharsets.UTF_8);

        var r = ATLauncherJsonPatcher.patch(f, "/opt/new/vida/vida-loader-0.1.0.jar",
                MV, "0.1.0");

        assertThat(r.alreadyAgent()).isTrue();
        assertThat(r.newArgs())
                .contains("-Xmx4G")
                .contains("-javaagent:/opt/jrebel.jar")
                .contains("-javaagent:/opt/new/vida/vida-loader-0.1.0.jar")
                .contains("-Dvida.loader.version=0.1.0")
                .doesNotContain("vida-loader-0.0.9");
    }

    @Test
    void rejects_paths_with_whitespace(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("instance.json");
        Files.writeString(f, "{\"id\":\"1.21.1\"}", StandardCharsets.UTF_8);

        assertThatThrownBy(() ->
                ATLauncherJsonPatcher.patch(f, "C:/Program Files/v.jar", MV, LV))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("whitespace");
    }

    @Test
    void rejects_non_object_json(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("instance.json");
        Files.writeString(f, "[\"not\",\"an\",\"object\"]", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ATLauncherJsonPatcher.patch(f, "/a.jar", MV, LV))
                .isInstanceOf(IOException.class);
    }
}

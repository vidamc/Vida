/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.atlauncher;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.InstallReport;
import dev.vida.installer.InstallerCore;
import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ATLauncherHandlerIT {

    private static Path prepareInstance(Path base) throws IOException {
        Path inst = base.resolve("instances/MyPack");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.json"), """
                {
                  "id":"1.21.1",
                  "launcher":{
                    "name":"My Awesome Pack",
                    "javaArguments":"-Xmx4G",
                    "loaderVersion":{"type":"Fabric","version":"0.15.11"}
                  }
                }
                """, StandardCharsets.UTF_8);
        return inst;
    }

    @SuppressWarnings("unchecked")
    @Test
    void patches_existing_instance_in_place(@TempDir Path base) throws IOException {
        Path inst = prepareInstance(base);
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.ATLAUNCHER)
                .installDir(base)
                .targetInstance(inst)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();

        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).as("errors: %s", rep.errors()).isTrue();

        assertThat(inst.resolve("vida/vida-loader-0.1.0.jar")).exists();
        assertThat(inst.resolve("vida/install.json")).exists();

        Map<String, Object> patched = (Map<String, Object>) JsonTree.parse(
                Files.readString(inst.resolve("instance.json"), StandardCharsets.UTF_8));
        Map<String, Object> launcher = (Map<String, Object>) patched.get("launcher");
        String args = (String) launcher.get("javaArguments");
        assertThat(args).contains("-Xmx4G");
        assertThat(args).contains("-javaagent:").contains("vida-loader-0.1.0.jar");
    }

    @Test
    void fails_without_target_instance(@TempDir Path base) {
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.ATLAUNCHER)
                .installDir(base)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();
        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).isFalse();
        assertThat(rep.errors().get(0)).containsIgnoringCase("target instance");
    }

    @Test
    void fails_when_instance_has_no_json(@TempDir Path base) throws IOException {
        Path inst = base.resolve("instances/Empty");
        Files.createDirectories(inst);
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.ATLAUNCHER)
                .installDir(base)
                .targetInstance(inst)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();
        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).isFalse();
        assertThat(rep.errors().get(0)).containsIgnoringCase("instance.json");
    }

    @Test
    void fails_when_path_has_whitespace(@TempDir Path base) throws IOException {
        Path instWithSpace = base.resolve("instances/My Pack");
        Files.createDirectories(instWithSpace);
        Files.writeString(instWithSpace.resolve("instance.json"),
                "{\"id\":\"1.21.1\",\"launcher\":{}}", StandardCharsets.UTF_8);
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.ATLAUNCHER)
                .installDir(base)
                .targetInstance(instWithSpace)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();
        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).isFalse();
        assertThat(rep.errors().get(0)).containsIgnoringCase("whitespace");
    }
}

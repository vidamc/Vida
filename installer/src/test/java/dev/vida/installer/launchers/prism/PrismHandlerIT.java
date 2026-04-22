/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PrismHandlerIT {

    @SuppressWarnings("unchecked")
    @Test
    void full_install_produces_prism_instance_layout(@TempDir Path data) throws IOException {
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.PRISM)
                .installDir(data)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();

        List<String> log = new ArrayList<>();
        InstallReport rep = new InstallerCore(log::add).install(opt);
        assertThat(rep.isOk()).as("errors: %s", rep.errors()).isTrue();

        Path inst = data.resolve("instances/vida-1.21.1-0.1.0");
        assertThat(inst).isDirectory();
        assertThat(inst.resolve("instance.cfg")).exists();
        assertThat(inst.resolve("mmc-pack.json")).exists();
        assertThat(inst.resolve("patches/dev.vida.loader.json")).exists();
        assertThat(inst.resolve("libraries/loader-0.1.0.jar")).exists();
        assertThat(inst.resolve(".minecraft/mods")).isDirectory();
        assertThat(inst.resolve("vida/install.json")).exists();

        String patchJson = Files.readString(inst.resolve("patches/dev.vida.loader.json"),
                StandardCharsets.UTF_8);
        Map<String, Object> patch = (Map<String, Object>) JsonTree.parse(patchJson);
        assertThat(patch.get("+agents")).as("Prism must use +agents").isInstanceOf(List.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void instance_name_override_respected(@TempDir Path data) throws IOException {
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.PRISM)
                .installDir(data)
                .instanceName("my custom name")
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();

        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).as("errors: %s", rep.errors()).isTrue();

        Path inst = data.resolve("instances/my-custom-name");
        assertThat(inst).isDirectory();

        String cfg = Files.readString(inst.resolve("instance.cfg"), StandardCharsets.UTF_8);
        assertThat(cfg).contains("name=my custom name");
    }

    @Test
    void reinstall_without_overwrite_fails(@TempDir Path data) throws IOException {
        var b = InstallOptions.builder()
                .launcherKind(LauncherKind.PRISM)
                .installDir(data)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0");

        assertThat(new InstallerCore(msg -> {}).install(b.build()).isOk()).isTrue();
        InstallReport second = new InstallerCore(msg -> {}).install(b.build());
        assertThat(second.isOk()).isFalse();
        assertThat(second.errors().get(0)).containsIgnoringCase("already exists");
    }

    @Test
    void fails_when_data_dir_missing(@TempDir Path parent) throws IOException {
        Path nonexistent = parent.resolve("nope");
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.PRISM)
                .installDir(nonexistent)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();
        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).isFalse();
        assertThat(rep.errors().get(0)).containsIgnoringCase("data dir does not exist");
    }
}

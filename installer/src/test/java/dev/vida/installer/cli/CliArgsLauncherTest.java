/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.cli;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.McDirDetector;
import dev.vida.installer.launchers.LauncherKind;
import org.junit.jupiter.api.Test;

final class CliArgsLauncherTest {

    private static final McDirDetector DETECTOR = new McDirDetector("Linux", "/home/ana", null);

    @Test
    void default_launcher_is_mojang() {
        var args = CliArgs.parse(new String[0], DETECTOR, "x");
        assertThat(args.options().launcherKind()).isEqualTo(LauncherKind.MOJANG);
    }

    @Test
    void launcher_prism_parsed() {
        var args = CliArgs.parse(new String[]{"--launcher", "prism", "--dir", "/tmp/prism"},
                DETECTOR, "x");
        assertThat(args.options().launcherKind()).isEqualTo(LauncherKind.PRISM);
        assertThat(args.options().installDir())
                .isEqualTo(java.nio.file.Paths.get("/tmp/prism"));
    }

    @Test
    void launcher_multimc_requires_dir() {
        assertThatThrownBy(() ->
                CliArgs.parse(new String[]{"--launcher", "multimc"}, DETECTOR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--dir is required");
    }

    @Test
    void launcher_atlauncher_parsed_with_instance() {
        var args = CliArgs.parse(new String[]{
                "--launcher", "atlauncher",
                "--dir", "/tmp/atl",
                "--instance", "/tmp/atl/instances/Foo"},
                DETECTOR, "x");
        assertThat(args.options().launcherKind()).isEqualTo(LauncherKind.ATLAUNCHER);
        assertThat(args.options().targetInstance().toString()).endsWith("Foo");
    }

    @Test
    void list_instances_is_an_action() {
        var args = CliArgs.parse(new String[]{"--launcher", "prism", "--dir", "/p",
                "--list-instances"}, DETECTOR, "x");
        assertThat(args.action()).isEqualTo(CliArgs.Action.LIST_INSTANCES);
    }

    @Test
    void instance_name_parsed() {
        var args = CliArgs.parse(new String[]{
                "--launcher", "prism", "--dir", "/p",
                "--instance-name", "my-vida"}, DETECTOR, "x");
        assertThat(args.options().instanceName()).isEqualTo("my-vida");
    }

    @Test
    void unknown_launcher_rejected() {
        assertThatThrownBy(() ->
                CliArgs.parse(new String[]{"--launcher", "nosuch"}, DETECTOR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown launcher");
    }

    @Test
    void modrinth_launcher_accepted() {
        var args = CliArgs.parse(new String[]{
                "--launcher", "modrinth", "--dir", "/p",
                "--instance", "/p/profiles/pack-1"}, DETECTOR, "x");
        assertThat(args.options().launcherKind()).isEqualTo(LauncherKind.MODRINTH);
    }

    @Test
    void curseforge_launcher_accepted() {
        var args = CliArgs.parse(new String[]{
                "--launcher", "curseforge", "--dir", "/p",
                "--instance", "/p/Instances/MyPack"}, DETECTOR, "x");
        assertThat(args.options().launcherKind()).isEqualTo(LauncherKind.CURSEFORGE);
    }

    @Test
    void launcher_alias_enum_name_also_accepted() {
        var args = CliArgs.parse(new String[]{"--launcher", "PRISM", "--dir", "/p"},
                DETECTOR, "x");
        assertThat(args.options().launcherKind()).isEqualTo(LauncherKind.PRISM);
    }
}

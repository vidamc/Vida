/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.cli;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.McDirDetector;
import org.junit.jupiter.api.Test;

final class CliArgsTest {

    private static final McDirDetector DEFAULT_DETECTOR =
            new McDirDetector("Linux", "/home/ana", null);

    @Test
    void defaults_pull_from_detector() {
        var args = CliArgs.parse(new String[0], DEFAULT_DETECTOR, "1.0.0");
        assertThat(args.action()).isEqualTo(CliArgs.Action.INSTALL);
        assertThat(args.headless()).isFalse();
        assertThat(args.options().installDir().toString()).endsWith(".minecraft");
        assertThat(args.options().minecraftVersion()).isEqualTo("1.21.1");
        assertThat(args.options().loaderVersion()).isEqualTo("1.0.0");
        assertThat(args.options().createLauncherProfile()).isTrue();
        assertThat(args.options().createLaunchScript()).isTrue();
        assertThat(args.options().dryRun()).isFalse();
        assertThat(args.options().overwrite()).isFalse();
    }

    @Test
    void headless_flag_detected() {
        var args = CliArgs.parse(new String[]{"--headless", "-y"},
                DEFAULT_DETECTOR, "x");
        assertThat(args.headless()).isTrue();
        assertThat(args.assumeYes()).isTrue();
    }

    @Test
    void help_short_and_long() {
        assertThat(CliArgs.parse(new String[]{"--help"}, DEFAULT_DETECTOR, "x").action())
                .isEqualTo(CliArgs.Action.HELP);
        assertThat(CliArgs.parse(new String[]{"-h"}, DEFAULT_DETECTOR, "x").action())
                .isEqualTo(CliArgs.Action.HELP);
    }

    @Test
    void dir_and_minecraft_and_loader_version() {
        var args = CliArgs.parse(new String[]{
                "--dir", "/tmp/mc",
                "--minecraft", "1.21.3",
                "--loader-version", "0.5.0"},
                DEFAULT_DETECTOR, "ignored");
        assertThat(args.options().installDir())
                .isEqualTo(java.nio.file.Paths.get("/tmp/mc"));
        assertThat(args.options().minecraftVersion()).isEqualTo("1.21.3");
        assertThat(args.options().loaderVersion()).isEqualTo("0.5.0");
    }

    @Test
    void positional_dir_accepted() {
        var args = CliArgs.parse(new String[]{"/tmp/mc"}, DEFAULT_DETECTOR, "x");
        assertThat(args.options().installDir())
                .isEqualTo(java.nio.file.Paths.get("/tmp/mc"));
    }

    @Test
    void multiple_positionals_rejected() {
        assertThatThrownBy(() ->
                CliArgs.parse(new String[]{"/tmp/a", "/tmp/b"}, DEFAULT_DETECTOR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected argument");
    }

    @Test
    void unknown_option_rejected() {
        assertThatThrownBy(() ->
                CliArgs.parse(new String[]{"--wat"}, DEFAULT_DETECTOR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown option");
    }

    @Test
    void unsupported_minecraft_version_rejected() {
        assertThatThrownBy(() ->
                CliArgs.parse(new String[]{"--minecraft", "1.20.4"}, DEFAULT_DETECTOR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Minecraft version");
    }

    @Test
    void missing_arg_rejected() {
        assertThatThrownBy(() ->
                CliArgs.parse(new String[]{"--dir"}, DEFAULT_DETECTOR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a path");
    }

    @Test
    void dry_run_and_overwrite_flags() {
        var args = CliArgs.parse(new String[]{
                "--dry-run", "--overwrite", "--no-launch-script"},
                DEFAULT_DETECTOR, "x");
        assertThat(args.options().dryRun()).isTrue();
        assertThat(args.options().overwrite()).isTrue();
        assertThat(args.options().createLaunchScript()).isFalse();
    }

    @Test
    void no_launcher_profile_flag() {
        var args = CliArgs.parse(new String[]{"--no-launcher-profile"},
                DEFAULT_DETECTOR, "x");
        assertThat(args.options().createLauncherProfile()).isFalse();
        assertThat(args.options().createLaunchScript()).isTrue();
    }

    @Test
    void help_text_is_not_empty() {
        assertThat(CliArgs.helpText()).contains("Usage").contains("--dir");
    }
}

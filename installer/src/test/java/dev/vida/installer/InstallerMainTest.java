/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class InstallerMainTest {

    @Test
    void contains_any_detects_known_flags() {
        assertThat(InstallerMain.containsAny(new String[]{"--headless"}, "--headless")).isTrue();
        assertThat(InstallerMain.containsAny(new String[]{"x", "-h"}, "--help", "-h")).isTrue();
        assertThat(InstallerMain.containsAny(new String[]{"x"}, "--headless")).isFalse();
        assertThat(InstallerMain.containsAny(new String[0], "--x")).isFalse();
    }

    @Test
    void version_returns_non_blank() {
        assertThat(InstallerMain.version()).isNotBlank();
    }

    @Test
    void headless_help_run_returns_zero_and_prints_usage() {
        var buf = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            int rc = InstallerMain.run(new String[]{"--headless", "--help"});
            assertThat(rc).isZero();
        } finally {
            System.setOut(original);
        }
        assertThat(buf.toString(StandardCharsets.UTF_8))
                .contains("Usage:").contains("--dir").contains("--minecraft");
    }

    @Test
    void headless_unknown_flag_returns_one() {
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        try {
            int rc = InstallerMain.run(new String[]{"--headless", "--wat"});
            assertThat(rc).isEqualTo(1);
        } finally {
            System.setErr(originalErr);
        }
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VigiaComandoTest {

    @TempDir Path tmp;

    @Test
    void no_args_shows_usage() {
        var cmd = new VigiaComando(tmp);
        String result = cmd.ejecutar(new String[0], null);
        assertThat(result).contains("Usage");
    }

    @Test
    void unknown_subcommand_shows_usage() {
        var cmd = new VigiaComando(tmp);
        String result = cmd.ejecutar(new String[]{"foo"}, null);
        assertThat(result).contains("Unknown subcommand");
    }

    @Test
    void start_stop_lifecycle() {
        var cmd = new VigiaComando(tmp);

        String startResult = cmd.ejecutar(new String[]{"start"}, null);
        assertThat(startResult).contains("started");

        String stopResult = cmd.ejecutar(new String[]{"stop"}, null);
        assertThat(stopResult).contains("stopped");
    }

    @Test
    void stop_without_start_gives_error() {
        var cmd = new VigiaComando(tmp);
        String result = cmd.ejecutar(new String[]{"stop"}, null);
        assertThat(result).contains("No active");
    }

    @Test
    void dump_without_start_gives_error() {
        var cmd = new VigiaComando(tmp);
        String result = cmd.ejecutar(new String[]{"dump"}, null);
        assertThat(result).contains("No active");
    }

    @Test
    void dump_during_active_session() {
        var cmd = new VigiaComando(tmp);
        cmd.ejecutar(new String[]{"start"}, null);

        String result = cmd.ejecutar(new String[]{"dump"}, null);
        assertThat(result).contains("Snapshot");

        cmd.ejecutar(new String[]{"stop"}, null);
    }

    @Test
    void double_start_gives_error() {
        var cmd = new VigiaComando(tmp);
        cmd.ejecutar(new String[]{"start"}, null);
        String result = cmd.ejecutar(new String[]{"start"}, null);
        assertThat(result).contains("already running");

        cmd.ejecutar(new String[]{"stop"}, null);
    }
}

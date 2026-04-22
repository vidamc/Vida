/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class VidaPremainTest {

    @Test
    void parses_agent_args() {
        BootOptions opts = VidaPremain.buildOptions(
                "modsDir=./mods,strict=true,skipDiscovery=yes");
        assertThat(opts.modsDir()).isEqualTo(Path.of("./mods"));
        assertThat(opts.strict()).isTrue();
        assertThat(opts.skipDiscovery()).isTrue();
    }

    @Test
    void null_and_empty_args_give_defaults() {
        BootOptions opts1 = VidaPremain.buildOptions(null);
        BootOptions opts2 = VidaPremain.buildOptions("");
        for (BootOptions o : new BootOptions[] { opts1, opts2 }) {
            assertThat(o.modsDir()).isNull();
            assertThat(o.strict()).isFalse();
            assertThat(o.skipDiscovery()).isFalse();
            assertThat(o.extraSources()).isEmpty();
            assertThat(o.gameJars()).isEmpty();
        }
    }

    @Test
    void ignores_malformed_pairs() {
        BootOptions opts = VidaPremain.buildOptions("=nokey,also,strict=true");
        assertThat(opts.strict()).isTrue();
    }

    @Test
    void parses_game_jars_list() {
        String sep = String.valueOf(File.pathSeparatorChar);
        BootOptions opts = VidaPremain.buildOptions(
                "gameJars=" + "a.jar" + sep + "b.jar" + sep + "c.jar");
        assertThat(opts.gameJars()).containsExactly(
                Path.of("a.jar"), Path.of("b.jar"), Path.of("c.jar"));
    }
}

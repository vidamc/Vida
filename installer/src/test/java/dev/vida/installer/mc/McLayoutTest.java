/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

final class McLayoutTest {

    @Test
    void derives_profile_id_and_paths() {
        Path mc = Paths.get("/tmp/mc");
        McLayout l = new McLayout(mc, "1.21.1", "0.5.0");

        assertThat(l.profileId()).isEqualTo("vida-1.21.1-0.5.0");
        assertThat(l.libraryRelativePath())
                .isEqualTo("dev/vida/vida-loader/0.5.0/vida-loader-0.5.0.jar");
        assertThat(l.libraryJar())
                .isEqualTo(Paths.get("/tmp/mc/libraries/dev/vida/vida-loader/0.5.0/vida-loader-0.5.0.jar"));
        assertThat(l.versionDir())
                .isEqualTo(Paths.get("/tmp/mc/versions/vida-1.21.1-0.5.0"));
        assertThat(l.versionJson())
                .isEqualTo(Paths.get("/tmp/mc/versions/vida-1.21.1-0.5.0/vida-1.21.1-0.5.0.json"));
        assertThat(l.versionJar())
                .isEqualTo(Paths.get("/tmp/mc/versions/vida-1.21.1-0.5.0/vida-1.21.1-0.5.0.jar"));
        assertThat(l.launcherProfiles())
                .isEqualTo(Paths.get("/tmp/mc/launcher_profiles.json"));
        assertThat(l.modsDir()).isEqualTo(Paths.get("/tmp/mc/mods"));
        assertThat(l.vidaDir()).isEqualTo(Paths.get("/tmp/mc/vida"));
        assertThat(l.vidaInstallJson()).isEqualTo(Paths.get("/tmp/mc/vida/install.json"));
    }

    @Test
    void display_name_and_maven_coord_are_human_readable() {
        McLayout l = new McLayout(Paths.get("/x"), "1.21.1", "0.5.0");
        assertThat(l.displayName()).isEqualTo("Vida 1.21.1 (0.5.0)");
        assertThat(l.loaderMavenCoord()).isEqualTo("dev.vida:vida-loader:0.5.0");
    }

    @Test
    void rejects_nulls() {
        assertThatThrownBy(() -> new McLayout(null, "x", "y"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new McLayout(Paths.get("/x"), null, "y"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new McLayout(Paths.get("/x"), "x", null))
                .isInstanceOf(NullPointerException.class);
    }
}

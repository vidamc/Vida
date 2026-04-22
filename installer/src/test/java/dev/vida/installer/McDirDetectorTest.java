/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

final class McDirDetectorTest {

    @Test
    void windows_uses_appdata() {
        var d = new McDirDetector("Windows 11", "C:\\Users\\Ana", "C:\\Users\\Ana\\AppData\\Roaming");
        // На разных ОС Paths.get нормализует по-разному — сравниваем с Paths.get.
        assertThat(d.defaultDir())
                .isEqualTo(Paths.get("C:\\Users\\Ana\\AppData\\Roaming", ".minecraft"));
    }

    @Test
    void windows_falls_back_to_home_when_appdata_missing() {
        var d = new McDirDetector("Windows", "C:\\Users\\Ana", null);
        assertThat(d.defaultDir().toString()).endsWith(".minecraft");
    }

    @Test
    void windows_falls_back_to_home_when_appdata_blank() {
        var d = new McDirDetector("Windows", "C:\\Users\\Ana", "   ");
        assertThat(d.defaultDir().toString()).endsWith(".minecraft");
    }

    @Test
    void macos_uses_library_app_support() {
        var d = new McDirDetector("Mac OS X", "/Users/ana", null);
        assertThat(d.defaultDir())
                .isEqualTo(Paths.get("/Users/ana", "Library", "Application Support", "minecraft"));
    }

    @Test
    void linux_uses_dotminecraft_in_home() {
        var d = new McDirDetector("Linux", "/home/ana", null);
        assertThat(d.defaultDir())
                .isEqualTo(Paths.get("/home/ana", ".minecraft"));
    }

    @Test
    void unknown_os_falls_back_to_linux_layout() {
        var d = new McDirDetector("Plan9", "/home/ana", null);
        assertThat(d.defaultDir())
                .isEqualTo(Paths.get("/home/ana", ".minecraft"));
    }
}

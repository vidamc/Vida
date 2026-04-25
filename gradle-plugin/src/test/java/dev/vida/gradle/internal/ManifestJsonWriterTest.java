/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.internal;

import static org.assertj.core.api.Assertions.*;

import dev.vida.manifest.ManifestParser;
import dev.vida.manifest.ModManifest;
import dev.vida.manifest.VifadaConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ManifestJsonWriterTest {

    @Test
    void minimal_manifest_round_trips_через_парсер() {
        String json = ManifestJsonWriter.toJson(
                1, "demo", "1.0.0", "Demo Mod",
                "", "", "",
                List.of(), List.of(), List.of(),
                Map.of(), Map.of(), List.of());

        var r = ManifestParser.parse(json);
        assertThat(r.isOk()).as("parse ok for %s", json).isTrue();
        ModManifest m = r.unwrap();
        assertThat(m.id()).isEqualTo("demo");
        assertThat(m.name()).isEqualTo("Demo Mod");
        assertThat(m.version().toString()).isEqualTo("1.0.0");
    }

    @Test
    void full_manifest_round_trips() {
        String json = ManifestJsonWriter.toJson(
                1, "miaventura", "0.1.0", "Mi Aventura",
                "A fine mod for testing", "Apache-2.0", "com.ejemplo.Ma",
                List.of("Ana", "Bob"),
                List.of("config/access.puertas"),
                List.of(),
                Map.of("vida", "^0.1", "otromod", "1.2.x"),
                Map.of("opcional", "0.1"),
                List.of("conflicto"));

        var r = ManifestParser.parse(json);
        assertThat(r.isOk()).as("parse ok for:\n%s", json).isTrue();
        ModManifest m = r.unwrap();
        assertThat(m.authors()).hasSize(2);
        assertThat(m.entrypoints().main()).containsExactly("com.ejemplo.Ma");
        assertThat(m.dependencies().required().keySet()).contains("vida", "otromod");
        assertThat(m.dependencies().optional().keySet()).contains("opcional");
        assertThat(m.puertas()).containsExactly("config/access.puertas");
        assertThat(m.incompatibilities()).containsExactly("conflicto");
    }

    @Test
    void special_chars_escaped() {
        String json = ManifestJsonWriter.toJson(
                1, "demo", "1.0.0", "Demo",
                "Line1\nLine2\tTabbed\"Quoted\\backslash", "", "",
                List.of(), List.of(), List.of(),
                Map.of(), Map.of(), List.of());
        var r = ManifestParser.parse(json);
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().description())
                .hasValue("Line1\nLine2\tTabbed\"Quoted\\backslash");
    }

    @Test
    void draft_manifest_supports_low_and_high_level_sections() {
        ManifestJsonWriter.Draft d = ManifestJsonWriter.Draft.builder()
                .schema(1)
                .id("full")
                .version("2.0.0")
                .name("Full")
                .description("")
                .license("MIT")
                .authors(List.of("A"))
                .entryPreLaunch(List.of("pkg.Pre"))
                .entryMain(List.of("pkg.Main"))
                .entryClient(List.of("pkg.Cli"))
                .entryServer(List.of("pkg.Srv"))
                .dependencies(Map.of("vida", "^1"))
                .puertas(List.of("meta/access.puertas"))
                .escultores(List.of("my.Escultor"))
                .vifadaPackages(List.of("my.mod.mixin"))
                .vifadaConfig("assets/full/full.vifada.json")
                .vifadaPriority(VifadaConfig.DEFAULT_PRIORITY + 10)
                .modules(List.of("vida-bloque"))
                .build();

        String json = ManifestJsonWriter.toJson(d);
        var r = ManifestParser.parse(json);
        assertThat(r.isOk()).as("%s", json).isTrue();
        ModManifest m = r.unwrap();
        assertThat(m.entrypoints().preLaunch()).containsExactly("pkg.Pre");
        assertThat(m.entrypoints().main()).containsExactly("pkg.Main");
        assertThat(m.entrypoints().client()).containsExactly("pkg.Cli");
        assertThat(m.entrypoints().server()).containsExactly("pkg.Srv");
        assertThat(m.vifada().packages()).containsExactly("my.mod.mixin");
        assertThat(m.vifada().config()).hasValue("assets/full/full.vifada.json");
        assertThat(m.vifada().priority()).isEqualTo(VifadaConfig.DEFAULT_PRIORITY + 10);
        assertThat(m.modules()).containsExactly("vida-bloque");
    }

    @Test
    void output_is_stable() {
        // Вызываем дважды — результат должен быть идентичным (детерминированная сериализация).
        String a = ManifestJsonWriter.toJson(
                1, "x", "1.0.0", "X", "", "", "",
                List.of("Z", "A", "M"), List.of(), List.of(),
                Map.of("b", "1", "a", "2"), Map.of(), List.of());
        String b = ManifestJsonWriter.toJson(
                1, "x", "1.0.0", "X", "", "", "",
                List.of("Z", "A", "M"), List.of(), List.of(),
                Map.of("b", "1", "a", "2"), Map.of(), List.of());
        assertThat(a).isEqualTo(b);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Version;
import dev.vida.manifest.ModManifest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NestedJarsTest {

    @Test
    void noCustomFieldMeansEmpty() {
        ModManifest m = ModManifest.builder("a", Version.parse("1.0.0"), "A").build();
        assertThat(NestedJars.from(m)).isEmpty();
    }

    @Test
    void extractsStringList() {
        ModManifest m = ModManifest.builder("a", Version.parse("1.0.0"), "A")
                .custom(Map.of("jars", List.of("META-INF/jars/inner.jar", "libs/other.jar")))
                .build();
        assertThat(NestedJars.from(m))
                .containsExactly("META-INF/jars/inner.jar", "libs/other.jar");
    }

    @Test
    void ignoresNonStringEntries() {
        ModManifest m = ModManifest.builder("a", Version.parse("1.0.0"), "A")
                .custom(Map.of("jars", List.of("ok.jar", 123L, "", "another.jar")))
                .build();
        assertThat(NestedJars.from(m)).containsExactly("ok.jar", "another.jar");
    }

    @Test
    void ignoresNonListValue() {
        ModManifest m = ModManifest.builder("a", Version.parse("1.0.0"), "A")
                .custom(Map.of("jars", "not-a-list"))
                .build();
        assertThat(NestedJars.from(m)).isEmpty();
    }
}

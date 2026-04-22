/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import dev.vida.discovery.ModSource;
import dev.vida.discovery.ZipReader;
import dev.vida.loader.internal.MorphCollector;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MorphCollectorTest {

    @Test
    void collects_morphs_from_jar(@TempDir Path dir) throws Exception {
        byte[] morph = TestSupport.buildHeadInjectMorph("demo/FooMorph", "demo.Foo");
        byte[] plain = TestSupport.buildPlainClass("demo/Util");

        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("vida.mod.json",
                TestSupport.mod("demo", "1.0.0").getBytes(StandardCharsets.UTF_8));
        entries.put("demo/FooMorph.class", morph);
        entries.put("demo/Util.class", plain);
        Path jar = TestSupport.writeJar(dir.resolve("demo.jar"), entries);

        MorphIndex.Builder b = MorphIndex.builder();
        try (ZipReader z = new ModSource.OnDisk(jar).open()) {
            int found = MorphCollector.collect(z, b);
            assertThat(found).isEqualTo(1);
        }
        MorphIndex idx = b.build();
        assertThat(idx.targets()).containsExactly("demo/Foo");
        assertThat(idx.forTarget("demo/Foo")).hasSize(1);
        assertThat(idx.forTarget("demo/Foo").get(0).internalName()).isEqualTo("demo/FooMorph");
    }

    @Test
    void ignores_non_class_entries_and_non_morph_classes(@TempDir Path dir) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("vida.mod.json",
                TestSupport.mod("x", "1.0.0").getBytes(StandardCharsets.UTF_8));
        entries.put("README.txt", "hi".getBytes(StandardCharsets.UTF_8));
        entries.put("demo/Regular.class", TestSupport.buildPlainClass("demo/Regular"));
        Path jar = TestSupport.writeJar(dir.resolve("x.jar"), entries);

        MorphIndex.Builder b = MorphIndex.builder();
        try (ZipReader z = new ModSource.OnDisk(jar).open()) {
            int found = MorphCollector.collect(z, b);
            assertThat(found).isZero();
        }
        assertThat(b.build().totalMorphs()).isZero();
    }

    @Test
    void readMorphTarget_on_non_morph_returns_null() {
        byte[] bytes = TestSupport.buildPlainClass("demo/Plain");
        assertThat(MorphCollector.readMorphTarget(bytes)).isNull();
        assertThat(MorphCollector.isMorph(bytes)).isFalse();
    }

    @Test
    void readMorphTarget_on_morph_returns_target_fqn() {
        byte[] bytes = TestSupport.buildHeadInjectMorph("a/B", "c.D");
        assertThat(MorphCollector.readMorphTarget(bytes)).isEqualTo("c.D");
        assertThat(MorphCollector.isMorph(bytes)).isTrue();
    }
}

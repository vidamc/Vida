/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Контракт изоляции: класс из JAR соседа не виден через чужой {@link ModLoader}. */
final class ModLoaderIsolationTest {

    private static URL[] classpathDir(Path dir, Map<String, byte[]> classes) throws Exception {
        Files.createDirectories(dir);
        for (var e : classes.entrySet()) {
            Path p = dir.resolve(e.getKey());
            Files.createDirectories(p.getParent());
            Files.write(p, e.getValue());
        }
        return new URL[] {dir.toUri().toURL()};
    }

    @Test
    void sibling_mod_class_not_visible_from_other_mod_loader(@TempDir Path dir) throws Exception {
        ClassLoader platform = ClassLoader.getPlatformClassLoader();
        VidaClassTransformer xform = new VidaClassTransformer(MorphIndex.empty(), e -> {});

        Path dirA = dir.resolve("mod-a");
        Path dirB = dir.resolve("mod-b");
        byte[] clsA = TestSupport.buildSimpleTarget("moda/Marker");
        byte[] clsB = TestSupport.buildSimpleTarget("modb/Secret");
        URL[] urlsA = classpathDir(dirA, Map.of("moda/Marker.class", clsA));
        URL[] urlsB = classpathDir(dirB, Map.of("modb/Secret.class", clsB));

        JuegoLoader juego = new JuegoLoader(new URL[0], platform, xform);
        try (ModLoader loaderA = new ModLoader("a", urlsA, juego, xform);
                ModLoader loaderB = new ModLoader("b", urlsB, juego, xform)) {
            assertThat(loaderB.loadClass("modb.Secret")).isNotNull();
            assertThatThrownBy(() -> loaderA.loadClass("modb.Secret"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }
}

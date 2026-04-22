/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import dev.vida.vifada.MorphSource;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class TransformingClassLoaderTest {

    /**
     * Используем classpath-директорию (а не JAR), чтобы на Windows
     * {@link org.junit.jupiter.api.io.TempDir} гарантированно мог удалить
     * содержимое после теста: URLClassLoader на Windows удерживает JAR
     * открытым дольше, чем длится try-with-resources.
     */
    private static URL[] classpathDir(Path dir, Map<String, byte[]> classes) throws Exception {
        Files.createDirectories(dir);
        for (var e : classes.entrySet()) {
            Path p = dir.resolve(e.getKey());
            Files.createDirectories(p.getParent());
            Files.write(p, e.getValue());
        }
        return new URL[] { dir.toUri().toURL() };
    }

    @Test
    void loads_and_transforms_class_on_findClass(@TempDir Path dir) throws Exception {
        byte[] target = TestSupport.buildSimpleTarget("x/Foo");
        Map<String, byte[]> cp = new LinkedHashMap<>();
        cp.put("x/Foo.class", target);
        URL[] urls = classpathDir(dir.resolve("cp"), cp);

        byte[] morph = TestSupport.buildHeadInjectMorph("x/FooMorph", "x.Foo");
        MorphIndex idx = MorphIndex.builder()
                .add("x/Foo", new MorphSource("x/FooMorph", morph))
                .build();
        VidaClassTransformer xform = new VidaClassTransformer(idx, e -> {});

        try (TransformingClassLoader cl = new TransformingClassLoader(
                "test", urls, getClass().getClassLoader(), xform)) {
            Class<?> foo = cl.loadClass("x.Foo");
            Object inst = foo.getDeclaredConstructor().newInstance();
            Method tick = foo.getMethod("tick");
            tick.invoke(inst);

            int counter = foo.getField("counter").getInt(inst);
            assertThat(counter).isEqualTo(2);
            assertThat(xform.transformedCount()).isEqualTo(1);
        }
    }

    @Test
    void delegates_self_classes_to_parent(@TempDir Path dir) throws Exception {
        URL[] urls = classpathDir(dir.resolve("cp"), Map.of());
        VidaClassTransformer xform = new VidaClassTransformer(MorphIndex.empty(), e -> {});

        try (TransformingClassLoader cl = new TransformingClassLoader(
                "test", urls, getClass().getClassLoader(), xform)) {
            Class<?> cb = cl.loadClass("dev.vida.vifada.CallbackInfo");
            // Не должен быть загружен нашим собственным loader'ом: это
            // «self-класс» и идёт через parent-цепочку (каков бы ни был
            // конкретный AppClassLoader / AppClassLoader.Child в Gradle).
            assertThat(cb.getClassLoader()).isNotSameAs(cl);
            // И это та же Class<?>, что и в parent-loader (identity).
            assertThat(cb).isSameAs(dev.vida.vifada.CallbackInfo.class);
        }
        assertThat(Files.exists(dir.resolve("cp"))).isTrue();
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Сквозные тесты: собираем, трансформируем, загружаем, вызываем. */
final class TransformerTest {

    private static TestSupport.ByteClassLoader defineTarget(byte[] transformed, String name) {
        TestSupport.ByteClassLoader cl = new TestSupport.ByteClassLoader(
                TransformerTest.class.getClassLoader());
        cl.put(name, transformed);
        return cl;
    }

    @Test
    void head_inject_runs_before_target_body() throws Exception {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo");
        byte[] morph = new TestSupport.MorphBuilder("test/FooMorph", "test.Foo").start()
                .shadowField("counter", "I")
                .inject("preTick", "tick()V", InjectionPoint.HEAD, "counter", false)
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/FooMorph", morph)));
        assertThat(r.errors()).as("errors=%s", r.errors()).isEmpty();
        assertThat(r.bytes()).isNotNull();

        TestSupport.ByteClassLoader cl = defineTarget(r.bytes(), "test/Foo");
        Class<?> c = cl.loadClass("test.Foo");
        Object inst = c.getDeclaredConstructor().newInstance();
        Method tick = c.getMethod("tick");
        tick.invoke(inst);

        // HEAD-инжект поднял counter на 1, потом оригинальное тело подняло ещё на 1.
        int counter = c.getField("counter").getInt(inst);
        assertThat(counter).isEqualTo(2);
    }

    @Test
    void head_inject_cancel_prevents_target_body() throws Exception {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo2");
        byte[] morph = new TestSupport.MorphBuilder("test/Foo2Morph", "test.Foo2").start()
                .shadowField("counter", "I")
                .inject("preTick", "tick()V", InjectionPoint.HEAD, "counter", true) // cancel=true
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/Foo2Morph", morph)));
        assertThat(r.errors()).isEmpty();

        TestSupport.ByteClassLoader cl = defineTarget(r.bytes(), "test/Foo2");
        Class<?> c = cl.loadClass("test.Foo2");
        Object inst = c.getDeclaredConstructor().newInstance();
        c.getMethod("tick").invoke(inst);

        // Инжект поднял на 1, оригинальное тело отменено → итог 1.
        assertThat(c.getField("counter").getInt(inst)).isEqualTo(1);
    }

    @Test
    void return_inject_runs_after_target_body() throws Exception {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo3");
        byte[] morph = new TestSupport.MorphBuilder("test/Foo3Morph", "test.Foo3").start()
                .shadowField("counter", "I")
                .inject("postTick", "tick()V", InjectionPoint.RETURN, "counter", false)
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/Foo3Morph", morph)));
        assertThat(r.errors()).isEmpty();

        TestSupport.ByteClassLoader cl = defineTarget(r.bytes(), "test/Foo3");
        Class<?> c = cl.loadClass("test.Foo3");
        Object inst = c.getDeclaredConstructor().newInstance();
        c.getMethod("tick").invoke(inst);

        // Тело +1, инжект перед return +1 → 2.
        assertThat(c.getField("counter").getInt(inst)).isEqualTo(2);
    }

    @Test
    void overwrite_replaces_method_body() throws Exception {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo4");
        byte[] morph = new TestSupport.MorphBuilder("test/Foo4Morph", "test.Foo4").start()
                .overwriteSum()
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/Foo4Morph", morph)));
        assertThat(r.errors()).isEmpty();

        TestSupport.ByteClassLoader cl = defineTarget(r.bytes(), "test/Foo4");
        Class<?> c = cl.loadClass("test.Foo4");
        Object inst = c.getDeclaredConstructor().newInstance();
        int res = (int) c.getMethod("sum", int.class, int.class).invoke(inst, 5, 3);

        // Оригинал: 5 + 3 = 8. Перезапись: 5 - 3 = 2.
        assertThat(res).isEqualTo(2);
    }

    @Test
    void two_head_injects_apply_in_priority_order() throws Exception {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo5");
        byte[] m1 = new TestSupport.MorphBuilder("test/M1", "test.Foo5").withPriority(100).start()
                .shadowField("counter", "I")
                .inject("a", "tick()V", InjectionPoint.HEAD, "counter", false).build();
        byte[] m2 = new TestSupport.MorphBuilder("test/M2", "test.Foo5").withPriority(200).start()
                .shadowField("counter", "I")
                .inject("b", "tick()V", InjectionPoint.HEAD, "counter", false).build();

        TransformReport r = Transformer.transform(target, List.of(
                new MorphSource("test/M1", m1),
                new MorphSource("test/M2", m2)));
        assertThat(r.errors()).isEmpty();

        TestSupport.ByteClassLoader cl = defineTarget(r.bytes(), "test/Foo5");
        Class<?> c = cl.loadClass("test.Foo5");
        Object inst = c.getDeclaredConstructor().newInstance();
        c.getMethod("tick").invoke(inst);

        // m1 +1, m2 +1, body +1 = 3
        assertThat(c.getField("counter").getInt(inst)).isEqualTo(3);
    }

    @Test
    void wrong_target_is_ignored_silently() {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo6");
        byte[] morph = new TestSupport.MorphBuilder("test/BadMorph", "test.DifferentTarget").start()
                .inject("x", "tick()V", InjectionPoint.HEAD, "counter", false)
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/BadMorph", morph)));
        assertThat(r.errors()).isEmpty();          // морф отфильтрован как "не про нас"
        assertThat(r.appliedMorphs()).isEmpty();   // ничего не применилось
        // Байты возвращены как есть.
        assertThat(r.bytes()).isEqualTo(target);
    }

    @Test
    void missing_target_method_gives_structured_error() {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo7");
        byte[] morph = new TestSupport.MorphBuilder("test/M7", "test.Foo7").start()
                .shadowField("counter", "I")
                .inject("x", "doesNotExist()V", InjectionPoint.HEAD, "counter", false)
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/M7", morph)));
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().get(0)).isInstanceOf(VifadaError.TargetMethodNotFound.class);
        VifadaError.TargetMethodNotFound e = (VifadaError.TargetMethodNotFound) r.errors().get(0);
        assertThat(e.methodSpec()).isEqualTo("doesNotExist()V");
    }

    @Test
    void missing_shadow_field_gives_structured_error() {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo8");
        byte[] morph = new TestSupport.MorphBuilder("test/M8", "test.Foo8").start()
                .shadowField("noSuchField", "Ljava/lang/String;")
                .build();

        TransformReport r = Transformer.transform(target, List.of(new MorphSource("test/M8", morph)));
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().get(0)).isInstanceOf(VifadaError.ShadowMissing.class);
    }

    @Test
    void morph_without_annotation_is_rejected_as_parse_error() {
        byte[] target = TestSupport.buildSimpleTarget("test/Foo9");
        var cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V21, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "test/NotAMorph", null, "java/lang/Object", null);
        cw.visitEnd();

        TransformReport r = Transformer.transform(target,
                List.of(new MorphSource("test/NotAMorph", cw.toByteArray())));
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().get(0)).isInstanceOf(VifadaError.NotAMorph.class);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import static org.assertj.core.api.Assertions.*;

import dev.vida.core.Result;
import dev.vida.vifada.internal.MorphDescriptor;
import dev.vida.vifada.internal.MorphParser;
import org.junit.jupiter.api.Test;

final class MorphParserTest {

    @Test
    void parses_target_and_priority() {
        byte[] bytes = new TestSupport.MorphBuilder("test/FooMorph", "test.Foo")
                .withPriority(500).start()
                .build();

        Result<MorphDescriptor, VifadaError> r = MorphParser.parse("test/FooMorph", bytes);
        assertThat(r.isOk()).isTrue();
        MorphDescriptor d = r.unwrap();
        assertThat(d.morphInternal).isEqualTo("test/FooMorph");
        assertThat(d.targetInternal).isEqualTo("test/Foo");
        assertThat(d.priority).isEqualTo(500);
        assertThat(d.isEmpty()).isTrue();
    }

    @Test
    void parses_shadow_field() {
        byte[] bytes = new TestSupport.MorphBuilder("test/S", "test.T").start()
                .shadowField("counter", "I")
                .build();

        var r = MorphParser.parse("test/S", bytes);
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().shadows).hasSize(1);
        assertThat(r.unwrap().shadows.get(0).name()).isEqualTo("counter");
        assertThat(r.unwrap().shadows.get(0).descriptor()).isEqualTo("I");
    }

    @Test
    void parses_inject_head_with_at() {
        byte[] bytes = new TestSupport.MorphBuilder("test/S", "test.T").start()
                .shadowField("counter", "I")
                .inject("onTick", "tick()V", InjectionPoint.HEAD, "counter", false)
                .build();

        var r = MorphParser.parse("test/S", bytes);
        assertThat(r.isOk()).isTrue();
        var d = r.unwrap();
        assertThat(d.injects).hasSize(1);
        assertThat(d.injects.get(0).targetMethod()).isEqualTo("tick()V");
        assertThat(d.injects.get(0).at()).isEqualTo(InjectionPoint.HEAD);
        assertThat(d.injects.get(0).requireTarget()).isTrue();
    }

    @Test
    void parses_overwrite() {
        byte[] bytes = new TestSupport.MorphBuilder("test/S", "test.T").start()
                .overwriteSum()
                .build();

        var r = MorphParser.parse("test/S", bytes);
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().overwrites).hasSize(1);
        assertThat(r.unwrap().overwrites.get(0).targetMethod()).isEqualTo("sum(II)I");
    }

    @Test
    void rejects_non_morph() {
        // Сгенерим пустой класс без @VifadaMorph.
        var cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V21, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "test/Plain", null, "java/lang/Object", null);
        cw.visitEnd();
        var r = MorphParser.parse("test/Plain", cw.toByteArray());
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(VifadaError.NotAMorph.class);
    }
}

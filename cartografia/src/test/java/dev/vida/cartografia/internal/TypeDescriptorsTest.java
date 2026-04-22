/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class TypeDescriptorsTest {

    @Test
    void primitivesAreMapped() {
        assertThat(TypeDescriptors.sourceToDescriptor("void")).isEqualTo("V");
        assertThat(TypeDescriptors.sourceToDescriptor("boolean")).isEqualTo("Z");
        assertThat(TypeDescriptors.sourceToDescriptor("byte")).isEqualTo("B");
        assertThat(TypeDescriptors.sourceToDescriptor("char")).isEqualTo("C");
        assertThat(TypeDescriptors.sourceToDescriptor("short")).isEqualTo("S");
        assertThat(TypeDescriptors.sourceToDescriptor("int")).isEqualTo("I");
        assertThat(TypeDescriptors.sourceToDescriptor("long")).isEqualTo("J");
        assertThat(TypeDescriptors.sourceToDescriptor("float")).isEqualTo("F");
        assertThat(TypeDescriptors.sourceToDescriptor("double")).isEqualTo("D");
    }

    @Test
    void classTypes() {
        assertThat(TypeDescriptors.sourceToDescriptor("java.lang.String"))
                .isEqualTo("Ljava/lang/String;");
        assertThat(TypeDescriptors.sourceToDescriptor("com.foo.bar.Baz$Inner"))
                .isEqualTo("Lcom/foo/bar/Baz$Inner;");
    }

    @Test
    void arrays() {
        assertThat(TypeDescriptors.sourceToDescriptor("int[]")).isEqualTo("[I");
        assertThat(TypeDescriptors.sourceToDescriptor("int[][]")).isEqualTo("[[I");
        assertThat(TypeDescriptors.sourceToDescriptor("java.lang.String[]"))
                .isEqualTo("[Ljava/lang/String;");
        assertThat(TypeDescriptors.sourceToDescriptor("a.b.C[][][]"))
                .isEqualTo("[[[La/b/C;");
    }

    @Test
    void methodDescriptorComposes() {
        String desc = TypeDescriptors.methodDescriptor(
                List.of("int", "java.lang.String", "boolean[]"),
                "java.util.Map");
        assertThat(desc).isEqualTo("(ILjava/lang/String;[Z)Ljava/util/Map;");
    }

    @Test
    void methodDescriptorWithNoParams() {
        assertThat(TypeDescriptors.methodDescriptor(List.of(), "void"))
                .isEqualTo("()V");
    }

    @Test
    void splitParamsHandlesEmpty() {
        assertThat(TypeDescriptors.splitParams("")).isEmpty();
        assertThat(TypeDescriptors.splitParams("   ")).isEmpty();
    }

    @Test
    void splitParamsHandlesMultiple() {
        assertThat(TypeDescriptors.splitParams("int, java.lang.String, float[]"))
                .containsExactly("int", "java.lang.String", "float[]");
    }

    @Test
    void splitParamsIgnoresGenericAngleCommas() {
        assertThat(TypeDescriptors.splitParams("java.util.Map<java.lang.String,java.lang.Integer>, int"))
                .containsExactly("java.util.Map<java.lang.String,java.lang.Integer>", "int");
    }

    @Test
    void rejectsEmpty() {
        assertThatThrownBy(() -> TypeDescriptors.sourceToDescriptor(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

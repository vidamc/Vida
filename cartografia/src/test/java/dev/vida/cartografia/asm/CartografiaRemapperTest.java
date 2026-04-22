/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.asm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import org.junit.jupiter.api.Test;

class CartografiaRemapperTest {

    private static MappingTree buildTree() {
        return MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "net/example/Foo")
                .addField("I", "a", "count")
                .addMethod("(I)V", "a", "setCount")
                .addMethod("()Labc;", "b", "copy")
                .done()
                .addClass("def", "net/example/Bar")
                .addMethod("(Labc;)V", "a", "configure")
                .done()
                .build();
    }

    @Test
    void mapClassRemapsKnownNames() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        assertThat(r.map("abc")).isEqualTo("net/example/Foo");
        assertThat(r.map("def")).isEqualTo("net/example/Bar");
    }

    @Test
    void mapClassLeavesUnknownUnchanged() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        assertThat(r.map("java/lang/String")).isEqualTo("java/lang/String");
        assertThat(r.map("net/other/Unknown")).isEqualTo("net/other/Unknown");
    }

    @Test
    void mapFieldAndMethod() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        assertThat(r.mapFieldName("abc", "a", "I")).isEqualTo("count");
        assertThat(r.mapMethodName("abc", "a", "(I)V")).isEqualTo("setCount");
        assertThat(r.mapMethodName("def", "a", "(Labc;)V")).isEqualTo("configure");
    }

    @Test
    void mapFieldWithUnknownOwnerIsIdentity() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        assertThat(r.mapFieldName("java/lang/String", "value", "[B")).isEqualTo("value");
    }

    @Test
    void mapMethodWithBadDescIsIdentity() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        assertThat(r.mapMethodName("abc", "a", "()V")).isEqualTo("a");
    }

    @Test
    void methodDescRemappingViaRemapper() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        // Remapper.mapMethodDesc translates class refs inside descriptor using map()
        assertThat(r.mapMethodDesc("(Labc;)V")).isEqualTo("(Lnet/example/Foo;)V");
        assertThat(r.mapDesc("[Ldef;")).isEqualTo("[Lnet/example/Bar;");
    }

    @Test
    void rejectsTargetNamespaceOutsideTree() {
        MappingTree tree = buildTree();
        assertThatThrownBy(() -> CartografiaRemapper.of(tree, Namespace.of("mojmap")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cachesClassNameLookups() {
        CartografiaRemapper r = CartografiaRemapper.of(buildTree(), Namespace.NAMED);
        // Мы не можем напрямую проверить, что кэш работает, но можно удостовериться
        // в идемпотентности и стабильности результата при многократных вызовах.
        for (int i = 0; i < 100; i++) {
            assertThat(r.map("abc")).isEqualTo("net/example/Foo");
            assertThat(r.map("java/lang/String")).isEqualTo("java/lang/String");
        }
    }
}

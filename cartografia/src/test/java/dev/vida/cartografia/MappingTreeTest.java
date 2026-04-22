/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MappingTreeTest {

    @Test
    void builderRequiresCorrectArity() {
        MappingTree.Builder b = MappingTree.builder(Namespace.OBF, Namespace.NAMED);
        assertThatThrownBy(() -> b.addClass("only/one"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateNamespaces() {
        assertThatThrownBy(() -> MappingTree.builder(Namespace.OBF, Namespace.OBF))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateSourceClass() {
        MappingTree.Builder b = MappingTree.builder(Namespace.OBF, Namespace.NAMED);
        b.addClass("abc", "Foo").done();
        assertThatThrownBy(() -> b.addClass("abc", "FooOther"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lookupBySourceAndTargetNamespace() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "net/minecraft/world/Foo").done()
                .addClass("def", "net/minecraft/world/Bar").done()
                .build();

        assertThat(tree.size()).isEqualTo(2);
        assertThat(tree.classBySource("abc").name(Namespace.NAMED))
                .isEqualTo("net/minecraft/world/Foo");
        assertThat(tree.classByName(Namespace.NAMED, "net/minecraft/world/Bar").sourceName())
                .isEqualTo("def");
        assertThat(tree.classBySource("nonexistent")).isNull();
    }

    @Test
    void fieldAndMethodLookup() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "net/example/Foo")
                .addField("I", "a", "count")
                .addField("Ljava/lang/String;", "b", "label")
                .addMethod("(I)V", "a", "setCount")
                .addMethod("(Ljava/lang/String;)V", "b", "setLabel")
                .done()
                .build();

        ClassMapping cm = tree.classBySource("abc");
        assertThat(cm.findField("a").name(Namespace.NAMED)).isEqualTo("count");
        assertThat(cm.findField("b").sourceDescriptor()).isEqualTo("Ljava/lang/String;");
        assertThat(cm.findMethod("a", "(I)V").name(Namespace.NAMED)).isEqualTo("setCount");
        assertThat(cm.findMethod("b", "(Ljava/lang/String;)V").sourceDescriptor())
                .isEqualTo("(Ljava/lang/String;)V");
        assertThat(cm.findMethod("a", "()V")).isNull();
    }

    @Test
    void remapDescriptorRewritesClassRefs() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "net/example/Foo").done()
                .addClass("def", "net/example/Bar").done()
                .build();

        String srcDesc = "(Labc;IJ)Ldef;";
        String mapped = tree.remapDescriptor(Namespace.OBF, Namespace.NAMED, srcDesc);
        assertThat(mapped).isEqualTo("(Lnet/example/Foo;IJ)Lnet/example/Bar;");
    }

    @Test
    void remapDescriptorLeavesUnknownClassesAlone() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "net/example/Foo").done()
                .build();

        String desc = "(Labc;Ljava/lang/String;)V";
        String mapped = tree.remapDescriptor(Namespace.OBF, Namespace.NAMED, desc);
        assertThat(mapped).isEqualTo("(Lnet/example/Foo;Ljava/lang/String;)V");
    }

    @Test
    void remapDescriptorPreservesArrays() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "net/example/Foo").done()
                .build();

        assertThat(tree.remapDescriptor(Namespace.OBF, Namespace.NAMED, "[[Labc;"))
                .isEqualTo("[[Lnet/example/Foo;");
    }

    @Test
    void remapDescriptorSameNamespaceIsIdentity() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED)
                .addClass("abc", "Foo").done()
                .build();

        assertThat(tree.remapDescriptor(Namespace.OBF, Namespace.OBF, "(Labc;)V"))
                .isEqualTo("(Labc;)V");
    }

    @Test
    void indexOfRecognizesNamespaces() {
        MappingTree tree = MappingTree.builder(Namespace.OBF, Namespace.NAMED).build();
        assertThat(tree.indexOf(Namespace.OBF)).isZero();
        assertThat(tree.indexOf(Namespace.NAMED)).isEqualTo(1);
        assertThat(tree.indexOf(Namespace.of("nope"))).isEqualTo(-1);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.FieldMapping;
import dev.vida.cartografia.MappingError;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.MethodMapping;
import dev.vida.cartografia.Namespace;
import dev.vida.core.Result;
import org.junit.jupiter.api.Test;

class ProguardReaderTest {

    private static final String SMALL = """
            # This is a comment
            com.mojang.blaze3d.Blaze3D -> aaa:
                int RENDER_START -> a
                java.lang.String description -> b
                1:2:void setClearColor(float,float,float,float) -> a
                void process(java.util.function.BooleanSupplier) -> b
            net.minecraft.world.Level -> lvl:
                com.mojang.blaze3d.Blaze3D blaze -> a
                void tick() -> a
                void addEntity(net.minecraft.world.Level,int) -> b
            """;

    @Test
    void parsesClassesAndMembers() {
        Result<MappingTree, MappingError> r = ProguardReader.readString(
                "small.txt", SMALL, Namespace.OBF, Namespace.NAMED);
        assertThat(r.isOk()).as("parse result %s", r).isTrue();

        MappingTree tree = r.unwrap();
        assertThat(tree.namespaces()).containsExactly(Namespace.OBF, Namespace.NAMED);
        assertThat(tree.size()).isEqualTo(2);

        ClassMapping blaze = tree.classBySource("aaa");
        assertThat(blaze.name(Namespace.NAMED)).isEqualTo("com/mojang/blaze3d/Blaze3D");

        FieldMapping f1 = blaze.findField("a");
        assertThat(f1.name(Namespace.NAMED)).isEqualTo("RENDER_START");
        assertThat(f1.sourceDescriptor()).isEqualTo("I");

        FieldMapping f2 = blaze.findField("b");
        assertThat(f2.name(Namespace.NAMED)).isEqualTo("description");
        assertThat(f2.sourceDescriptor()).isEqualTo("Ljava/lang/String;");

        MethodMapping m1 = blaze.findMethod("a", "(FFFF)V");
        assertThat(m1).isNotNull();
        assertThat(m1.name(Namespace.NAMED)).isEqualTo("setClearColor");

        MethodMapping m2 = blaze.findMethod("b", "(Ljava/util/function/BooleanSupplier;)V");
        assertThat(m2).isNotNull();
        assertThat(m2.name(Namespace.NAMED)).isEqualTo("process");
    }

    @Test
    void rewritesInnerClassRefsInDescriptors() {
        Result<MappingTree, MappingError> r = ProguardReader.readString(
                "mut.txt", SMALL, Namespace.OBF, Namespace.NAMED);
        assertThat(r.isOk()).isTrue();
        MappingTree tree = r.unwrap();

        ClassMapping level = tree.classBySource("lvl");
        assertThat(level.name(Namespace.NAMED)).isEqualTo("net/minecraft/world/Level");

        // Поле типа Blaze3D должно в source-ns иметь дескриптор с obf-именем.
        FieldMapping blazeField = level.findField("a");
        assertThat(blazeField.name(Namespace.NAMED)).isEqualTo("blaze");
        assertThat(blazeField.sourceDescriptor()).isEqualTo("Laaa;");

        // Метод с параметром-Level должен иметь obf-класс в дескрипторе.
        MethodMapping addEntity = level.findMethod("b", "(Llvl;I)V");
        assertThat(addEntity).isNotNull();
        assertThat(addEntity.name(Namespace.NAMED)).isEqualTo("addEntity");
    }

    @Test
    void emptyInputProducesEmptyTree() {
        Result<MappingTree, MappingError> r = ProguardReader.readString(
                "empty.txt", "# just a comment\n\n", Namespace.OBF, Namespace.NAMED);
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().size()).isZero();
    }

    @Test
    void syntaxErrorOnMalformedLine() {
        String bad = """
                com.example.Foo -> obf:
                    int broken_field_without_arrow
                """;
        Result<MappingTree, MappingError> r = ProguardReader.readString(
                "bad.txt", bad, Namespace.OBF, Namespace.NAMED);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(MappingError.SyntaxError.class);
    }

    @Test
    void memberOutsideClassIsError() {
        String bad = "    int field -> a\n";
        Result<MappingTree, MappingError> r = ProguardReader.readString(
                "bad2.txt", bad, Namespace.OBF, Namespace.NAMED);
        assertThat(r.isErr()).isTrue();
        assertThat(((MappingError.SyntaxError) r.unwrapErr()).detail())
                .contains("without enclosing class");
    }

    @Test
    void classHeaderWithoutArrowIsError() {
        String bad = "com.example.Foo :\n";
        Result<MappingTree, MappingError> r = ProguardReader.readString(
                "bad3.txt", bad, Namespace.OBF, Namespace.NAMED);
        assertThat(r.isErr()).isTrue();
    }
}

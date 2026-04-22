/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.MappingError;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.core.Result;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class CtgRoundtripTest {

    private static MappingTree sample() {
        return MappingTree.builder(Namespace.OBF, Namespace.NAMED, Namespace.of("intermediate"))
                .addClass("abc", "net/example/Foo", "inter/Foo1")
                .addField("I", "a", "count", "f1")
                .addField("Ljava/lang/String;", "b", "label", "f2")
                .addMethod("(I)V", "a", "setCount", "m1")
                .addMethod("(Ljava/lang/String;)V", "b", "setLabel", "m2")
                .done()
                .addClass("def", "net/example/Bar", "inter/Bar1")
                .addField("Labc;", "a", "foo", "fx")
                .addMethod("(Labc;)V", "a", "configure", "mx")
                .done()
                .build();
    }

    @Test
    void roundtripPreservesStructure() {
        MappingTree original = sample();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Result<Void, MappingError> wr = CtgWriter.write(original, baos);
        assertThat(wr.isOk()).as("write: %s", wr).isTrue();

        Result<MappingTree, MappingError> rd = CtgReader.read(
                "roundtrip.ctg", new ByteArrayInputStream(baos.toByteArray()));
        assertThat(rd.isOk()).as("read: %s", rd).isTrue();

        MappingTree copy = rd.unwrap();
        assertThat(copy.namespaces()).isEqualTo(original.namespaces());
        assertThat(copy.size()).isEqualTo(original.size());

        ClassMapping origFoo = original.classBySource("abc");
        ClassMapping copyFoo = copy.classBySource("abc");
        assertThat(copyFoo.name(Namespace.NAMED)).isEqualTo(origFoo.name(Namespace.NAMED));
        assertThat(copyFoo.name(Namespace.of("intermediate")))
                .isEqualTo(origFoo.name(Namespace.of("intermediate")));
        assertThat(copyFoo.findField("a").sourceDescriptor()).isEqualTo("I");
        assertThat(copyFoo.findField("a").name(Namespace.NAMED)).isEqualTo("count");
        assertThat(copyFoo.findMethod("b", "(Ljava/lang/String;)V").name(Namespace.NAMED))
                .isEqualTo("setLabel");

        ClassMapping copyBar = copy.classBySource("def");
        assertThat(copyBar.findField("a").sourceDescriptor()).isEqualTo("Labc;");
        assertThat(copyBar.findMethod("a", "(Labc;)V").name(Namespace.NAMED))
                .isEqualTo("configure");
    }

    @Test
    void badMagicIsRejected() {
        byte[] bogus = new byte[] {'N', 'O', 'P', 'E', 0, 0, 0, 0, 0, 0, 0, 0};
        Result<MappingTree, MappingError> rd = CtgReader.read(
                "bad.ctg", new ByteArrayInputStream(bogus));
        assertThat(rd.isErr()).isTrue();
        assertThat(rd.unwrapErr()).isInstanceOf(MappingError.Corrupted.class);
    }

    @Test
    void unsupportedVersionIsRejected() {
        // VIDACTG\n + major=2 + minor=0 + flags=0 + bogus rest
        byte[] headerBad = new byte[] {
                'V', 'I', 'D', 'A', 'C', 'T', 'G', '\n',
                2, 0, 0, 0,
        };
        Result<MappingTree, MappingError> rd = CtgReader.read(
                "v2.ctg", new ByteArrayInputStream(headerBad));
        assertThat(rd.isErr()).isTrue();
        assertThat(rd.unwrapErr()).isInstanceOf(MappingError.UnsupportedVersion.class);
    }

    @Test
    void emptyTreeRoundtrips() {
        MappingTree empty = MappingTree.builder(Namespace.OBF, Namespace.NAMED).build();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThat(CtgWriter.write(empty, baos).isOk()).isTrue();
        Result<MappingTree, MappingError> rd = CtgReader.read(
                "empty.ctg", new ByteArrayInputStream(baos.toByteArray()));
        assertThat(rd.isOk()).isTrue();
        MappingTree copy = rd.unwrap();
        assertThat(copy.size()).isZero();
        assertThat(copy.namespaces()).containsExactly(Namespace.OBF, Namespace.NAMED);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.discovery.internal.Fingerprints;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModsIndexRoundtripTest {

    private static ModsIndex sample() {
        byte[] sha = Fingerprints.sha256("content".getBytes());
        ModsIndex.Entry top = new ModsIndex.Entry(
                "e:/mods/foo.jar", 0, 1_700_000_000_000L, 12345L, sha,
                "foo", "1.2.3", List.of("META-INF/jars/inner.jar"));
        ModsIndex.Entry inner = new ModsIndex.Entry(
                "e:/mods/foo.jar!/META-INF/jars/inner.jar", 1, 0L, 4096L,
                Fingerprints.sha256("inner".getBytes()),
                "inner", "0.9.1", List.of());
        return new ModsIndex(Instant.ofEpochMilli(1_700_000_500_000L), List.of(top, inner));
    }

    @Test
    void roundtripsThroughStream() throws IOException {
        ModsIndex idx = sample();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ModsIndexWriter.write(idx, baos);

        ModsIndex back = ModsIndexReader.read(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(back.size()).isEqualTo(idx.size());
        assertThat(back.writtenAt()).isEqualTo(idx.writtenAt());

        ModsIndex.Entry top = back.get("e:/mods/foo.jar");
        assertThat(top).isNotNull();
        assertThat(top.depth()).isZero();
        assertThat(top.modId()).isEqualTo("foo");
        assertThat(top.modVersion()).isEqualTo("1.2.3");
        assertThat(top.mtimeMillis()).isEqualTo(1_700_000_000_000L);
        assertThat(top.sizeBytes()).isEqualTo(12345L);
        assertThat(top.nestedInnerPaths()).containsExactly("META-INF/jars/inner.jar");
        assertThat(top.sha256Hex()).isEqualTo(Fingerprints.hex(Fingerprints.sha256("content".getBytes())));

        ModsIndex.Entry inner = back.get("e:/mods/foo.jar!/META-INF/jars/inner.jar");
        assertThat(inner).isNotNull();
        assertThat(inner.depth()).isEqualTo(1);
        assertThat(inner.modId()).isEqualTo("inner");
        assertThat(inner.nestedInnerPaths()).isEmpty();
    }

    @Test
    void atomicFileRoundtrip(@TempDir Path tmp) throws IOException {
        ModsIndex idx = sample();
        Path p = tmp.resolve("mods.idx");
        ModsIndexWriter.writeAtomic(idx, p);

        ModsIndex back = ModsIndexReader.readFile(p);
        assertThat(back.size()).isEqualTo(idx.size());
    }

    @Test
    void badMagicIsRejected() {
        byte[] bogus = {'N', 'O', 'P', 'E', 0, 0, 0, 0, 1, 0, 0, 0};
        assertThatThrownBy(() -> ModsIndexReader.read(new ByteArrayInputStream(bogus)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("bad magic");
    }

    @Test
    void unsupportedVersionIsRejected() {
        // magic + major=2
        byte[] bad = {
                'V', 'I', 'D', 'A', 'I', 'D', 'X', '\n',
                2, 0, 0, 0, // major=2
        };
        assertThatThrownBy(() -> ModsIndexReader.read(new ByteArrayInputStream(bad)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unsupported version");
    }

    @Test
    void emptyIndexRoundtrips() throws IOException {
        ModsIndex empty = new ModsIndex(Instant.ofEpochMilli(1L), List.of());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ModsIndexWriter.write(empty, baos);
        ModsIndex back = ModsIndexReader.read(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(back.size()).isZero();
    }

    @Test
    void entryRejectsBadSha() {
        assertThatThrownBy(() -> new ModsIndex.Entry(
                "src", 0, 0, 0, new byte[10], "mod", "1.0.0", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

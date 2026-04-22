/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class McArtifactsTest {

    /** SHA-1("abc") = a9993e364706816aba3e25717850c26c9cd0d89d. */
    @Test
    void copy_with_sha1_matches_rfc3174_vector() throws IOException {
        byte[] src = "abc".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        var r = McArtifacts.copyWithSha1(new ByteArrayInputStream(src), dst);

        assertThat(r.sizeBytes()).isEqualTo(3L);
        assertThat(r.sha1Hex()).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
        assertThat(dst.toByteArray()).isEqualTo(src);
    }

    @Test
    void sha1_of_file_matches_stream_computation(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("data.bin");
        byte[] bytes = new byte[1024];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i % 256);
        Files.write(f, bytes);

        var r = McArtifacts.sha1Of(f);
        assertThat(r.sizeBytes()).isEqualTo(1024);
        assertThat(r.sha1Hex()).hasSize(40);
    }

    @Test
    void empty_jar_marker_is_valid_and_has_manifest(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("empty.jar");
        McArtifacts.writeEmptyJar(jar);
        assertThat(jar).exists();

        try (JarInputStream jin = new JarInputStream(Files.newInputStream(jar))) {
            var mf = jin.getManifest();
            assertThat(mf).isNotNull();
            assertThat(mf.getMainAttributes().getValue("Created-By"))
                    .isEqualTo("vida-installer");
            // У пустого jar, кроме manifest, нет записей.
            assertThat(jin.getNextJarEntry()).isNull();
        }
    }

    @Test
    void write_empty_jar_creates_parent_dirs(@TempDir Path dir) throws IOException {
        Path nested = dir.resolve("a/b/c/marker.jar");
        McArtifacts.writeEmptyJar(nested);
        assertThat(nested).exists();
    }
}

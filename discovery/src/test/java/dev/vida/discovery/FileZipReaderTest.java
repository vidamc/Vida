/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileZipReaderTest {

    @Test
    void readsFromDiskArchive(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("sub/tiny.jar");
        TestJars.writeToDisk(jar, Map.of(
                "a.txt", TestJars.utf8("alpha"),
                "b/inner.txt", TestJars.utf8("beta")));

        try (FileZipReader r = new FileZipReader(jar)) {
            assertThat(r.source()).isEqualTo(jar.toString());
            assertThat(r.entries()).containsExactlyInAnyOrder("a.txt", "b/inner.txt");
            assertThat(new String(r.read("a.txt"), StandardCharsets.UTF_8)).isEqualTo("alpha");
            try (InputStream is = r.open("b/inner.txt")) {
                byte[] bytes = is.readAllBytes();
                assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("beta");
            }
        }
    }

    @Test
    void missingEntry(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("one.jar");
        TestJars.writeToDisk(jar, Map.of("only.txt", TestJars.utf8("x")));
        try (FileZipReader r = new FileZipReader(jar)) {
            assertThatThrownBy(() -> r.read("nope"))
                    .isInstanceOf(java.io.FileNotFoundException.class);
        }
    }

    @Test
    void closeDoesNotThrow(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("one.jar");
        TestJars.writeToDisk(jar, Map.of("only.txt", TestJars.utf8("x")));
        FileZipReader r = new FileZipReader(jar);
        r.close(); // must not raise
        // Повторный close не должен падать.
        r.close();
    }
}

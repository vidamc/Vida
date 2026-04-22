/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BytesZipReaderTest {

    @Test
    void readsEntries() throws IOException {
        byte[] zip = TestJars.buildBytes(Map.of(
                "hello.txt", TestJars.utf8("Hi"),
                "deep/path/foo.txt", TestJars.utf8("bar")));
        try (BytesZipReader r = new BytesZipReader("test.zip", zip)) {
            assertThat(r.entries()).containsExactlyInAnyOrder("hello.txt", "deep/path/foo.txt");
            assertThat(r.contains("hello.txt")).isTrue();
            assertThat(r.contains("nope")).isFalse();
            assertThat(new String(r.read("hello.txt"), StandardCharsets.UTF_8)).isEqualTo("Hi");
        }
    }

    @Test
    void readsAreDefensiveCopies() throws IOException {
        byte[] zip = TestJars.buildBytes(Map.of("a.txt", TestJars.utf8("one")));
        try (BytesZipReader r = new BytesZipReader("t.zip", zip)) {
            byte[] first = r.read("a.txt");
            first[0] = 'X';
            byte[] second = r.read("a.txt");
            assertThat(new String(second, StandardCharsets.UTF_8)).isEqualTo("one");
        }
    }

    @Test
    void missingEntryThrows() throws IOException {
        byte[] zip = TestJars.buildBytes(Map.of("a.txt", TestJars.utf8("one")));
        try (BytesZipReader r = new BytesZipReader("t.zip", zip)) {
            assertThatThrownBy(() -> r.read("missing"))
                    .isInstanceOf(java.io.FileNotFoundException.class);
        }
    }

    @Test
    void ignoresDirectoryEntries() throws IOException {
        // Создадим вручную zip с каталогом внутри.
        byte[] zip = TestJars.buildBytes(Map.of(
                "dir/", new byte[0],
                "dir/file.txt", TestJars.utf8("ok")));
        try (BytesZipReader r = new BytesZipReader("t.zip", zip)) {
            assertThat(r.entries()).containsExactly("dir/file.txt");
        }
    }
}

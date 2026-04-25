/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VifadaMorphTraceHtmlTest {

    @AfterEach
    void cleanup() {
        VifadaMorphTraceHtml.resetForTests();
    }

    @Test
    void html_contains_rows(@TempDir Path dir) throws Exception {
        TransformReport rep = new TransformReport(
                new byte[] {1, 2, 3},
                List.of("dev/foo/Morph1", "dev/foo/Morph2"),
                List.of(),
                List.of());
        VifadaMorphTraceHtml.registrar("net/minecraft/client/Foo", rep);
        Path out = dir.resolve("trace.html");
        VifadaMorphTraceHtml.flushNowForTests(out);
        String html = Files.readString(out);
        assertThat(html).contains("net/minecraft/client/Foo");
        assertThat(html).contains("Morph1");
    }
}

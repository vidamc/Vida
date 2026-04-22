/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VigiaReporteTest {

    @Test
    void renderizar_produces_valid_html() {
        Resumen resumen = new Resumen(
                Duration.ofSeconds(30), 1500,
                List.of(
                        new Resumen.MetodoMuestra("net.minecraft.Server#tick", 300, 20.0),
                        new Resumen.MetodoMuestra("java.lang.Thread#sleep", 200, 13.3)),
                List.of(
                        new Resumen.LatidoMetrica("vida:pulso", 4, 600)),
                3, 2, 150);

        String html = VigiaReporte.renderizar(resumen);

        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html).contains("<title>Vida Vigia");
        assertThat(html).contains("net.minecraft.Server#tick");
        assertThat(html).contains("300");
        assertThat(html).contains("20.0%");
        assertThat(html).contains("vida:pulso");
        assertThat(html).contains("1500");
        assertThat(html).contains("</html>");
    }

    @Test
    void renderizar_handles_empty_resumen() {
        Resumen resumen = new Resumen(
                Duration.ofSeconds(1), 0,
                List.of(), List.of(), 0, 0, 0);

        String html = VigiaReporte.renderizar(resumen);

        assertThat(html).contains("No execution samples collected");
        assertThat(html).contains("<!DOCTYPE html>");
    }

    @Test
    void escribir_creates_file(@TempDir Path tmp) throws IOException {
        Resumen resumen = new Resumen(
                Duration.ofSeconds(10), 100,
                List.of(new Resumen.MetodoMuestra("a.B#c", 50, 50.0)),
                List.of(), 0, 0, 0);

        Path out = tmp.resolve("report.html");
        Path written = VigiaReporte.escribir(resumen, out);

        assertThat(written).isEqualTo(out);
        assertThat(Files.exists(out)).isTrue();
        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertThat(content).contains("a.B#c");
    }

    @Test
    void nombreArchivo_has_timestamp_pattern() {
        String name = VigiaReporte.nombreArchivo();
        assertThat(name).startsWith("vigia-report-");
        assertThat(name).endsWith(".html");
        assertThat(name).matches("vigia-report-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.html");
    }

    @Test
    void html_escapes_special_chars() {
        Resumen resumen = new Resumen(
                Duration.ofSeconds(1), 10,
                List.of(new Resumen.MetodoMuestra("<script>alert('xss')</script>", 5, 50.0)),
                List.of(), 0, 0, 0);

        String html = VigiaReporte.renderizar(resumen);
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }
}

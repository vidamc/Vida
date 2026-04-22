/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Генерирует HTML-отчёт из {@link Resumen}.
 *
 * <p>Отчёт — самодостаточный HTML с inline-CSS, flame-chart на CSS-барах,
 * top-20 методов и breakdown по Susurro.
 */
@ApiStatus.Preview("vigia")
public final class VigiaReporte {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .withZone(ZoneId.systemDefault());

    private VigiaReporte() {}

    /**
     * Рендерит Resumen в HTML-строку.
     */
    public static String renderizar(Resumen resumen) {
        Objects.requireNonNull(resumen, "resumen");
        var sb = new StringBuilder(4096);
        sb.append("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Vida Vigia — Profiling Report</title>
                <style>
                *{box-sizing:border-box;margin:0;padding:0}
                body{font-family:system-ui,-apple-system,sans-serif;background:#0d1117;color:#c9d1d9;padding:2rem}
                h1{color:#58a6ff;margin-bottom:.5rem}
                h2{color:#79c0ff;margin:1.5rem 0 .5rem;border-bottom:1px solid #21262d;padding-bottom:.3rem}
                .summary{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:1rem;margin:1rem 0}
                .card{background:#161b22;border:1px solid #30363d;border-radius:8px;padding:1rem}
                .card .label{font-size:.85rem;color:#8b949e}
                .card .value{font-size:1.6rem;font-weight:700;color:#58a6ff}
                table{width:100%;border-collapse:collapse;margin:.5rem 0}
                th,td{text-align:left;padding:.5rem .75rem;border-bottom:1px solid #21262d}
                th{color:#8b949e;font-weight:600;font-size:.85rem;text-transform:uppercase}
                .bar-bg{background:#21262d;border-radius:4px;overflow:hidden;height:20px}
                .bar-fg{height:100%;border-radius:4px;background:linear-gradient(90deg,#238636,#2ea043)}
                .pct{font-size:.85rem;color:#8b949e;min-width:60px;text-align:right}
                footer{margin-top:2rem;color:#484f58;font-size:.8rem}
                </style>
                </head>
                <body>
                <h1>🔍 Vida Vigia — Profiling Report</h1>
                """);

        sb.append("<div class=\"summary\">");
        card(sb, "Duration", formatDuration(resumen.duracion()));
        card(sb, "Samples", String.valueOf(resumen.muestras()));
        card(sb, "Susurro Active", String.valueOf(resumen.susurroActivos()));
        card(sb, "Susurro Pending", String.valueOf(resumen.susurroPendientes()));
        card(sb, "Susurro Completed", String.valueOf(resumen.susurroCompletadas()));
        sb.append("</div>");

        sb.append("<h2>Top Methods (by sample count)</h2>");
        if (resumen.topMetodos().isEmpty()) {
            sb.append("<p>No execution samples collected.</p>");
        } else {
            sb.append("<table><thead><tr><th>#</th><th>Method</th><th>Samples</th><th>%</th><th></th></tr></thead><tbody>");
            int rank = 1;
            for (var m : resumen.topMetodos()) {
                sb.append("<tr><td>").append(rank++).append("</td>");
                sb.append("<td><code>").append(escapeHtml(m.metodo())).append("</code></td>");
                sb.append("<td>").append(m.muestras()).append("</td>");
                sb.append("<td class=\"pct\">").append(String.format(Locale.ROOT, "%.1f%%", m.porcentaje())).append("</td>");
                sb.append("<td style=\"width:40%\"><div class=\"bar-bg\"><div class=\"bar-fg\" style=\"width:")
                        .append(String.format(Locale.ROOT, "%.1f", m.porcentaje()))
                        .append("%\"></div></div></td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        if (!resumen.latidoMetricas().isEmpty()) {
            sb.append("<h2>Latido (Event Bus) Metrics</h2>");
            sb.append("<table><thead><tr><th>Channel</th><th>Subscribers</th><th>Emissions</th></tr></thead><tbody>");
            for (var lm : resumen.latidoMetricas()) {
                sb.append("<tr><td><code>").append(escapeHtml(lm.nombre())).append("</code></td>");
                sb.append("<td>").append(lm.suscriptores()).append("</td>");
                sb.append("<td>").append(lm.emisiones()).append("</td></tr>");
            }
            sb.append("</tbody></table>");
        }

        sb.append("<footer>Generated by Vida Vigia at ")
                .append(TS_FMT.format(Instant.now()))
                .append("</footer></body></html>\n");

        return sb.toString();
    }

    /**
     * Рендерит и записывает в файл. Имя файла по конвенции:
     * {@code vigia-report-<timestamp>.html}.
     *
     * @param resumen  данные профилирования
     * @param destino  путь к HTML-файлу
     * @return записанный путь
     */
    public static Path escribir(Resumen resumen, Path destino) throws IOException {
        Objects.requireNonNull(destino, "destino");
        String html = renderizar(resumen);
        Path parent = destino.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(destino, html, StandardCharsets.UTF_8);
        return destino;
    }

    /** Генерирует стандартное имя файла: {@code vigia-report-2026-04-22_14-30-00.html}. */
    public static String nombreArchivo() {
        return "vigia-report-" + TS_FMT.format(Instant.now()) + ".html";
    }

    // ================================================================

    private static void card(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"card\"><div class=\"label\">")
                .append(escapeHtml(label))
                .append("</div><div class=\"value\">")
                .append(escapeHtml(value))
                .append("</div></div>");
    }

    private static String formatDuration(java.time.Duration d) {
        long sec = d.getSeconds();
        if (sec < 60) return sec + "s";
        long min = sec / 60;
        return min + "m " + (sec % 60) + "s";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Накопитель трассировки применения морфов и экспорт в HTML (включается JVM-свойством
 * {@code -Dvida.vifada.trace.html=&lt;path&gt;}).
 */
@ApiStatus.Stable
public final class VifadaMorphTraceHtml {

    private static final CopyOnWriteArrayList<Fila> FILAS = new CopyOnWriteArrayList<>();
    private static volatile boolean hookInstalled;

    private VifadaMorphTraceHtml() {}

    /** Регистрирует одну успешную трансформацию (внутренний класс цели + список морфов). */
    public static void registrar(String internalNameTarget, TransformReport report) {
        if (report == null || report.appliedMorphs().isEmpty()) {
            return;
        }
        FILAS.add(new Fila(
                System.currentTimeMillis(),
                internalNameTarget,
                String.join(", ", report.appliedMorphs()),
                report.errors().size(),
                0L));
        Path out = pathSalida();
        if (out != null) {
            ensureShutdownHook(out);
        }
    }

    private static Path pathSalida() {
        String p = System.getProperty("vida.vifada.trace.html");
        if (p == null || p.isBlank()) {
            return null;
        }
        return Path.of(p.trim());
    }

    private static synchronized void ensureShutdownHook(Path out) {
        if (hookInstalled) {
            return;
        }
        hookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> flushTo(out), "vida-vifada-trace-flush"));
    }

    /** Сбрасывает накопленные строки в HTML (для тестов и ручного вызова). */
    @ApiStatus.Internal
    public static void flushNowForTests(Path out) {
        flushTo(out);
    }

    static void flushTo(Path out) {
        List<Fila> snap = new ArrayList<>(FILAS);
        if (snap.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(10_000);
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/><title>Vifada morph trace</title>")
                .append("<style>table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:4px}")
                .append("</style></head><body><h1>Vifada morph trace</h1><table><tr>")
                .append("<th>time_ms</th><th>target_internal</th><th>applied_morphs</th>")
                .append("<th>errors</th><th>reserved</th></tr>");
        for (Fila f : snap) {
            sb.append("<tr><td>")
                    .append(f.tiempoMs)
                    .append("</td><td>")
                    .append(escape(f.objetivo))
                    .append("</td><td>")
                    .append(escape(f.aplicados))
                    .append("</td><td>")
                    .append(f.errores)
                    .append("</td><td>")
                    .append(f.reservado)
                    .append("</td></tr>");
        }
        sb.append("</table></body></html>");
        try {
            Path parent = out.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // best-effort diagnostic export
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    record Fila(long tiempoMs, String objetivo, String aplicados, int errores, long reservado) {}

    /** Только для тестов. */
    @ApiStatus.Internal
    public static void resetForTests() {
        FILAS.clear();
        hookInstalled = false;
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.susurro.Susurro;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Контракт команды {@code /vida profile start|stop|dump}.
 *
 * <p>Регистрируется loader'ом при инициализации; выполнение идёт через
 * {@link #ejecutar(String[], Susurro)}. Результат — текстовый вывод
 * для чата или консоли.
 *
 * <p>Допустимые субкоманды:
 * <ul>
 *   <li>{@code start} — начинает новую сессию профилирования.</li>
 *   <li>{@code stop} — останавливает текущую сессию, сохраняет {@code .jfr},
 *       генерирует HTML-отчёт.</li>
 *   <li>{@code dump} — создаёт снапшот текущей сессии без остановки.</li>
 * </ul>
 */
@ApiStatus.Stable
public final class VigiaComando {

    private static final Log LOG = Log.of(VigiaComando.class);

    private volatile VigiaSesion sesionActual;
    private final Path outputDir;

    /**
     * @param outputDir директория для отчётов (обычно {@code .minecraft/vida/vigia/})
     */
    public VigiaComando(Path outputDir) {
        this.outputDir = Objects.requireNonNull(outputDir, "outputDir");
    }

    /**
     * Выполняет субкоманду.
     *
     * @param args    аргументы после {@code /vida profile}
     * @param susurro пул Susurro для статистики (nullable)
     * @return текстовое сообщение для пользователя
     */
    public String ejecutar(String[] args, Susurro susurro) {
        if (args == null || args.length == 0) {
            return "Usage: /vida profile <start|stop|dump>";
        }
        return switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "start" -> iniciar(susurro);
            case "stop"  -> detener();
            case "dump"  -> instantanea();
            default -> "Unknown subcommand: " + args[0]
                    + ". Usage: /vida profile <start|stop|dump>";
        };
    }

    private String iniciar(Susurro susurro) {
        if (sesionActual != null && sesionActual.activa()) {
            return "§cProfiler is already running. Stop it first with '/vida profile stop'.";
        }
        try {
            VigiaSesion sesion = VigiaSesion.iniciar();
            if (susurro != null) sesion.conSusurro(susurro);
            this.sesionActual = sesion;
            return "§aProfiler started. Use '/vida profile stop' to finish.";
        } catch (IllegalStateException e) {
            LOG.error("Failed to start profiler", e);
            return "§cFailed to start profiler: " + e.getMessage();
        }
    }

    private String detener() {
        VigiaSesion sesion = this.sesionActual;
        if (sesion == null || !sesion.activa()) {
            return "§cNo active profiling session.";
        }
        try {
            String ts = VigiaReporte.nombreArchivo().replace(".html", "");
            Path jfrFile = outputDir.resolve(ts + ".jfr");
            Resumen resumen = sesion.detener(jfrFile);
            this.sesionActual = null;

            Path htmlFile = outputDir.resolve(ts + ".html");
            VigiaReporte.escribir(resumen, htmlFile);

            return "§aProfiler stopped. "
                    + resumen.muestras() + " samples collected.\n"
                    + "§7JFR: " + jfrFile.toAbsolutePath() + "\n"
                    + "§7HTML: " + htmlFile.toAbsolutePath();
        } catch (IOException e) {
            LOG.error("Failed to stop profiler", e);
            return "§cFailed to stop profiler: " + e.getMessage();
        }
    }

    private String instantanea() {
        VigiaSesion sesion = this.sesionActual;
        if (sesion == null || !sesion.activa()) {
            return "§cNo active profiling session.";
        }
        Resumen resumen = sesion.instantanea();
        return "§aSnapshot: " + resumen.muestras() + " samples, "
                + resumen.topMetodos().size() + " unique methods.\n"
                + "§7Top method: "
                + (resumen.topMetodos().isEmpty() ? "N/A"
                : resumen.topMetodos().getFirst().metodo()
                + " (" + String.format("%.1f%%", resumen.topMetodos().getFirst().porcentaje()) + ")");
    }
}

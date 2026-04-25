/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.susurro.Susurro;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

/**
 * Лёгкая сессия профилирования поверх JDK Flight Recorder.
 *
 * <p>Жизненный цикл:
 * <ol>
 *   <li>{@link #iniciar()} — запускает JFR-запись с конфигурацией «profile».</li>
 *   <li>{@link #instantanea()} — создаёт {@link Resumen} без остановки записи.</li>
 *   <li>{@link #detener(Path)} — останавливает запись, сбрасывает {@code .jfr}-файл
 *       и возвращает финальный {@link Resumen}.</li>
 * </ol>
 *
 * <p>Потокобезопасность: все публичные методы синхронизированы через
 * {@link AtomicReference} на состоянии.
 *
 * <h2>Пример</h2>
 * <pre>{@code
 * VigiaSesion sesion = VigiaSesion.iniciar();
 * // ... выполнение нагрузки ...
 * Resumen res = sesion.detener(Path.of("vigia-report.jfr"));
 * String html = VigiaReporte.renderizar(res);
 * }</pre>
 */
@ApiStatus.Stable
public final class VigiaSesion {

    private static final Log LOG = Log.of(VigiaSesion.class);
    private static final int TOP_METODOS = 20;

    private enum Estado { ACTIVA, DETENIDA }

    private final Recording recording;
    private final Instant inicio;
    private final AtomicReference<Estado> estado = new AtomicReference<>(Estado.ACTIVA);
    private volatile Susurro susurro;

    private VigiaSesion(Recording recording) {
        this.recording = recording;
        this.inicio = Instant.now();
    }

    /**
     * Inicia una nueva sesión de profiling JFR.
     *
     * @throws IllegalStateException si JFR no está disponible
     */
    public static VigiaSesion iniciar() {
        if (!FlightRecorder.isAvailable()) {
            throw new IllegalStateException(
                    "JDK Flight Recorder no está disponible en esta JVM (use un JDK completo "
                            + "y compruebe que JFR no esté desactivado por políticas de seguridad)");
        }
        try {
            Configuration config = Configuration.getConfiguration("profile");
            Recording rec = new Recording(config);
            rec.start();
            LOG.info("Vigia profiling session started");
            return new VigiaSesion(rec);
        } catch (Exception e) {
            throw new IllegalStateException("No se puede iniciar JFR: " + e.getMessage(), e);
        }
    }

    /** Asocia un pool Susurro para captura de estadísticas. */
    public void conSusurro(Susurro s) {
        this.susurro = Objects.requireNonNull(s, "susurro");
    }

    /**
     * Crea una instantánea sin detener la sesión.
     * Los datos son aproximados (requiere flush a archivo temporal).
     */
    public Resumen instantanea() {
        verificarActiva();
        try {
            Path tmp = Files.createTempFile("vigia-snap-", ".jfr");
            try {
                recording.dump(tmp);
                return analizarJfr(tmp, Duration.between(inicio, Instant.now()));
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            LOG.warn("Error creando instantánea: {}", e.getMessage());
            return resumenVacio();
        }
    }

    /**
     * Detiene la sesión y escribe el archivo JFR.
     *
     * @param destino ruta del archivo {@code .jfr} de salida
     * @return resumen final
     */
    public Resumen detener(Path destino) throws IOException {
        if (!estado.compareAndSet(Estado.ACTIVA, Estado.DETENIDA)) {
            throw new IllegalStateException("La sesión ya está detenida");
        }
        Objects.requireNonNull(destino, "destino");
        Duration duracion = Duration.between(inicio, Instant.now());

        recording.stop();
        Path parent = destino.getParent();
        if (parent != null) Files.createDirectories(parent);
        recording.dump(destino);
        recording.close();

        LOG.info("Vigia profiling session stopped, wrote {}", destino);
        return analizarJfr(destino, duracion);
    }

    /** Detiene la sesión sin guardar archivo. */
    public void cancelar() {
        if (estado.compareAndSet(Estado.ACTIVA, Estado.DETENIDA)) {
            recording.stop();
            recording.close();
            LOG.info("Vigia profiling session cancelled");
        }
    }

    public boolean activa() {
        return estado.get() == Estado.ACTIVA;
    }

    // ================================================================

    private Resumen analizarJfr(Path jfrFile, Duration duracion) {
        Map<String, Long> muestrasPorMetodo = new HashMap<>();
        long totalMuestras = 0;

        try (RecordingFile rf = new RecordingFile(jfrFile)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                if (!"jdk.ExecutionSample".equals(event.getEventType().getName())) {
                    continue;
                }
                totalMuestras++;
                RecordedStackTrace stack = event.getStackTrace();
                if (stack == null || stack.getFrames().isEmpty()) continue;

                RecordedFrame top = stack.getFrames().getFirst();
                String key = top.getMethod().getType().getName()
                        + "#" + top.getMethod().getName();
                muestrasPorMetodo.merge(key, 1L, Long::sum);
            }
        } catch (IOException e) {
            LOG.warn("Error al analizar JFR: {}", e.getMessage());
        }

        long finalTotal = totalMuestras;
        List<Resumen.MetodoMuestra> topMetodos = muestrasPorMetodo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_METODOS)
                .map(e -> new Resumen.MetodoMuestra(
                        e.getKey(),
                        e.getValue(),
                        finalTotal > 0 ? (e.getValue() * 100.0 / finalTotal) : 0.0))
                .toList();

        int susActivos = 0, susPendientes = 0;
        long susCompletadas = 0;
        Susurro s = this.susurro;
        if (s != null) {
            Susurro.Estadisticas est = s.estadisticas();
            susActivos = est.activos();
            susPendientes = est.pendientes();
            susCompletadas = est.completadas();
        }

        return new Resumen(
                duracion, totalMuestras, topMetodos,
                List.of(),
                susActivos, susPendientes, susCompletadas);
    }

    private Resumen resumenVacio() {
        return new Resumen(
                Duration.between(inicio, Instant.now()),
                0, List.of(), List.of(),
                0, 0, 0);
    }

    private void verificarActiva() {
        if (estado.get() != Estado.ACTIVA) {
            throw new IllegalStateException("La sesión no está activa");
        }
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.telemetry;

import dev.vida.core.ApiStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Telemetría agregada opcional (opt-in). Sin red por defecto; solo contadores locales.
 *
 * <p>Habilitar con {@code -Dvida.telemetry.enabled=true}. No recolecta PII ni rutas absolutas.
 */
@ApiStatus.Stable
public final class TelemetriaV1 {

    private static final AtomicLong ARRANQUE_FRIO_NS = new AtomicLong(-1);
    private static final LongAdder TICKS_LIBRES = new LongAdder();
    private static final LongAdder NANOS_TRANSFORM_MORPH = new LongAdder();

    private TelemetriaV1() {}

    /** @return si la telemetría está habilitada por JVM */
    public static boolean habilitada() {
        return Boolean.parseBoolean(System.getProperty("vida.telemetry.enabled", "false"));
    }

    /** Registra duración nominal de cold-start del loader (nanosegundos). */
    public static void registrarArranqueFrioNanos(long nanos) {
        if (!habilitada()) {
            return;
        }
        ARRANQUE_FRIO_NS.compareAndSet(-1, nanos);
    }

    /** Incremento de “freezes” detectados (evento discreto definido por el llamador). */
    public static void cuentaTickLibre() {
        if (!habilitada()) {
            return;
        }
        TICKS_LIBRES.increment();
    }

    /** Nanosegundos totales dedicados a transformadores de morfos (suma agregada). */
    public static void agregaNanosTransformMorph(long nanos) {
        if (!habilitada()) {
            return;
        }
        NANOS_TRANSFORM_MORPH.add(Math.max(0L, nanos));
    }

    /** Reinicia acumuladores (solo tests; no altera {@link #habilitada()}). */
    static void limpiarParaPruebas() {
        ARRANQUE_FRIO_NS.set(-1);
        TICKS_LIBRES.reset();
        NANOS_TRANSFORM_MORPH.reset();
    }

    /** Snapshot solo con claves conocidas — apto para logs públicos (sin identidad). */
    public static Map<String, Object> snapshotSinPii() {
        Map<String, Object> out = new LinkedHashMap<>();
        long cold = ARRANQUE_FRIO_NS.get();
        if (cold >= 0) {
            out.put("coldStartNanos", cold);
        }
        out.put("freezeEvents", TICKS_LIBRES.sum());
        out.put("transformMorphNanosTotal", NANOS_TRANSFORM_MORPH.sum());
        return Map.copyOf(out);
    }

}

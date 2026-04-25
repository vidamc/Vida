/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Счётчики вызовов Escultor при старте (define-time трансформации).
 *
 * <p>Поле «наносекунды на Escultor» заполняется только если включено свойство
 * JVM {@code -Dvida.loader.profileEscultorNanos=true}; иначе в агрегаты
 * пишется {@code 0} — счётчики попаданий остаются без накладных {@code nanoTime}.
 */
@ApiStatus.Internal
public final class EscultorRegistroMetricas {

    private final ConcurrentHashMap<String, LongAdder> intentos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> modificados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> nanosTotal = new ConcurrentHashMap<>();

    public void registro(String nombreEscultor, long nanos, boolean cambio) {
        Objects.requireNonNull(nombreEscultor, "nombreEscultor");
        intentos.computeIfAbsent(nombreEscultor, k -> new LongAdder()).increment();
        nanosTotal.computeIfAbsent(nombreEscultor, k -> new LongAdder()).add(Math.max(0L, nanos));
        if (cambio) {
            modificados.computeIfAbsent(nombreEscultor, k -> new LongAdder()).increment();
        }
    }

    public Map<String, Long> intentosPorNombre() {
        return Map.copyOf(intentos.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().sum())));
    }

    public Map<String, Long> modificacionesPorNombre() {
        return Map.copyOf(modificados.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().sum())));
    }

    public Map<String, Long> nanosPorNombre() {
        return Map.copyOf(nanosTotal.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().sum())));
    }
}

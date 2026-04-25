/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.core.ApiStatus;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Реестр путей настроек, помеченных {@link Ajuste.Builder#sincronizar()}, для построения
 * серверного снимка, пересылаемого клиенту (модуль {@code vida-red}).
 */
@ApiStatus.Stable
public final class AjustesSincronizacionCatalogo {

    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static final Set<String> RUTAS = new LinkedHashSet<>();

    private AjustesSincronizacionCatalogo() {}

    static void registrar(Ajuste<?> ajuste) {
        Objects.requireNonNull(ajuste, "ajuste");
        LOCK.writeLock().lock();
        try {
            RUTAS.add(ajuste.ruta());
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /** Неизменяемый снимок зарегистрированных путей (порядок вставки). */
    public static List<String> rutasRegistradas() {
        LOCK.readLock().lock();
        try {
            return List.copyOf(RUTAS);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static boolean contieneRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return false;
        }
        LOCK.readLock().lock();
        try {
            return RUTAS.contains(ruta);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    /** Только для тестов. */
    @ApiStatus.Internal
    public static void resetForTests() {
        LOCK.writeLock().lock();
        try {
            RUTAS.clear();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /** Фильтрует карту, оставляя только зарегистрированные пути. */
    public static java.util.Map<String, String> filtrar(java.util.Map<String, String> candidatos) {
        Objects.requireNonNull(candidatos, "candidatos");
        LOCK.readLock().lock();
        try {
            java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
            for (var e : candidatos.entrySet()) {
                if (RUTAS.contains(e.getKey())) {
                    out.put(e.getKey(), e.getValue());
                }
            }
            return java.util.Map.copyOf(out);
        } finally {
            LOCK.readLock().unlock();
        }
    }

}

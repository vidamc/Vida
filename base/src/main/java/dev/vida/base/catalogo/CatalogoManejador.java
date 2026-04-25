/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Центральный реестр реестров: один на рантайм Vida, хранит по одному
 * {@link CatalogoMutable} на каждый {@code reestroId}.
 *
 * <p>Методы thread-safe (основаны на {@link ConcurrentHashMap}). Типовой
 * рабочий процесс:
 *
 * <pre>{@code
 *   // На этапе инициализации мода:
 *   CatalogoMutable<Bloque> bloques = ctx.catalogos().abrir(
 *           Identifier.of("ejemplo", "block"), Bloque.class);
 *
 *   bloques.registrar(CatalogoClave.de("ejemplo:block", "ejemplo:espada"),
 *           new Bloque(...));
 *
 *   // Позже ядро замораживает и отдаёт remove-only view:
 *   ctx.catalogos().congelarTodo();
 * }</pre>
 */
@ApiStatus.Stable
public final class CatalogoManejador {

    private final ConcurrentHashMap<Identifier, DefaultCatalogo<?>> catalogos = new ConcurrentHashMap<>();

    /** Открывает (или возвращает уже открытый) реестр с заданным id и типом. */
    @SuppressWarnings("unchecked")
    public <T> CatalogoMutable<T> abrir(Identifier reestroId, Class<T> claseValor) {
        Objects.requireNonNull(reestroId, "reestroId");
        Objects.requireNonNull(claseValor, "claseValor");
        DefaultCatalogo<?> existente = catalogos.computeIfAbsent(reestroId,
                id -> new DefaultCatalogo<>(id, claseValor));
        if (!existente.claseValor().equals(claseValor)) {
            throw new ClassCastException("registry " + reestroId
                    + " already opened with value type " + existente.claseValor().getName()
                    + ", requested " + claseValor.getName());
        }
        return (CatalogoMutable<T>) existente;
    }

    /** Открывает реестр с id в виде строки. */
    public <T> CatalogoMutable<T> abrir(String reestroId, Class<T> claseValor) {
        return abrir(Identifier.parse(reestroId), claseValor);
    }

    /** Просмотр существующего реестра, если он открыт и совместим по типу. */
    @SuppressWarnings("unchecked")
    public <T> Optional<Catalogo<T>> obtener(Identifier reestroId, Class<T> claseValor) {
        DefaultCatalogo<?> c = catalogos.get(reestroId);
        if (c == null) return Optional.empty();
        if (!c.claseValor().equals(claseValor)) return Optional.empty();
        return Optional.of((Catalogo<T>) c);
    }

    /** Сколько реестров открыто. */
    public int cantidad() { return catalogos.size(); }

    /** Все id открытых реестров. */
    public Collection<Identifier> ids() {
        return List.copyOf(catalogos.keySet());
    }

    /** Замораживает все реестры. */
    public void congelarTodo() {
        for (DefaultCatalogo<?> c : catalogos.values()) c.congelar();
    }

    /** Сбрасывает всё (для тестов). */
    public void limpiar() {
        catalogos.clear();
    }

    /**
     * Сбрасывает открытые реестры в процессе разработки с
     * {@code -Dvida.dev.hotReload=true} (см. {@code vidaRun} / hot-reload).
     */
    public void reiniciarParaHotReloadDesarrollo() {
        if (!Boolean.getBoolean("vida.dev.hotReload")) {
            throw new UnsupportedOperationException(
                    "reiniciarParaHotReloadDesarrollo requires -Dvida.dev.hotReload=true");
        }
        limpiar();
    }

    /** Иммутабельный снимок всех реестров. */
    public Map<Identifier, Catalogo<?>> snapshotTodo() {
        Map<Identifier, Catalogo<?>> out = new java.util.LinkedHashMap<>(catalogos.size());
        for (var e : catalogos.entrySet()) out.put(e.getKey(), e.getValue().snapshot());
        return Map.copyOf(out);
    }
}

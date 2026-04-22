/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.core.Result;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Потокобезопасная in-memory реализация {@link CatalogoMutable}.
 *
 * <p>Устойчива к записям до и после заморозки: после
 * {@link #congelar()} запись возвращает {@link CatalogoError.CatalogoCerrado}
 * и список записей становится иммутабельным для чтения (последние
 * снапшоты переиспользуются).
 */
@ApiStatus.Preview("base")
public final class DefaultCatalogo<T> implements CatalogoMutable<T> {

    private final Identifier reestroId;
    private final Class<T> claseValor;
    /** Стабильный порядок регистрации + O(1) по ключу. */
    private final Map<CatalogoClave<T>, Inscripcion<T>> porClave = new LinkedHashMap<>();
    private final List<Inscripcion<T>> porNumerico = new ArrayList<>();
    private final Map<CatalogoClave<T>, T> valores = new LinkedHashMap<>();

    private volatile boolean congelado = false;

    public DefaultCatalogo(Identifier reestroId, Class<T> claseValor) {
        this.reestroId = Objects.requireNonNull(reestroId, "reestroId");
        this.claseValor = Objects.requireNonNull(claseValor, "claseValor");
    }

    public static <T> DefaultCatalogo<T> de(String reestroId, Class<T> clase) {
        return new DefaultCatalogo<>(Identifier.parse(reestroId), clase);
    }

    // ================================================================

    @Override public Identifier reestroId() { return reestroId; }
    @Override public Class<T>   claseValor() { return claseValor; }
    @Override public boolean    congelado() { return congelado; }

    @Override
    public synchronized int tamanio() {
        return porClave.size();
    }

    @Override
    public synchronized boolean contiene(CatalogoClave<T> clave) {
        return porClave.containsKey(clave);
    }

    @Override
    public synchronized Optional<T> obtener(CatalogoClave<T> clave) {
        return Optional.ofNullable(valores.get(clave));
    }

    @Override
    public synchronized Optional<T> obtener(int numerico) {
        if (numerico < 0 || numerico >= porNumerico.size()) return Optional.empty();
        return porNumerico.get(numerico).valor();
    }

    @Override
    public synchronized Optional<Integer> numericoDe(CatalogoClave<T> clave) {
        Inscripcion<T> i = porClave.get(clave);
        return i == null ? Optional.empty() : Optional.of(i.numerico());
    }

    @Override
    public synchronized Collection<CatalogoClave<T>> claves() {
        return List.copyOf(porClave.keySet());
    }

    @Override
    public synchronized Collection<T> valores() {
        return List.copyOf(valores.values());
    }

    @Override
    public synchronized Collection<Inscripcion<T>> inscripciones() {
        return List.copyOf(porClave.values());
    }

    // ================================================================

    @Override
    public synchronized Result<Inscripcion<T>, CatalogoError> registrar(CatalogoClave<T> clave, T valor) {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(valor, "valor");

        if (congelado) {
            return Result.err(new CatalogoError.CatalogoCerrado(reestroId.toString()));
        }
        if (!clave.reestroId().equals(reestroId)) {
            return Result.err(new CatalogoError.ClaveAjena(reestroId.toString(),
                    clave.reestroId().toString()));
        }
        if (!claseValor.isInstance(valor)) {
            // Возвращаем ClaveAjena с расширённым контекстом — не хотим плодить
            // ошибки на каждый край; это внутренняя ошибка типа и обычно
            // отражает баг мода.
            throw new ClassCastException("value " + valor.getClass().getName()
                    + " is not an instance of " + claseValor.getName());
        }
        Inscripcion<T> existente = porClave.get(clave);
        if (existente != null) {
            return Result.err(new CatalogoError.ClaveDuplicada(clave, existente.toString()));
        }
        int numerico = porNumerico.size();
        Inscripcion<T> ins = new Inscripcion<>(this, clave, numerico);
        porClave.put(clave, ins);
        porNumerico.add(ins);
        valores.put(clave, valor);
        return Result.ok(ins);
    }

    @Override
    public synchronized void congelar() {
        congelado = true;
    }

    @Override
    public String toString() {
        return "Catalogo(" + reestroId + ", " + tamanio() + " entries"
                + (congelado ? ", congelado" : "") + ")";
    }

    // ================================================================
    //                 Статический read-only view
    // ================================================================

    /** Неизменяемый снимок для передачи во внешние компоненты. */
    public synchronized Catalogo<T> snapshot() {
        return new Snapshot<>(reestroId, claseValor,
                Collections.unmodifiableMap(new LinkedHashMap<>(porClave)),
                List.copyOf(porNumerico),
                Collections.unmodifiableMap(new LinkedHashMap<>(valores)));
    }

    private static final class Snapshot<T> implements Catalogo<T> {
        private final Identifier reestroId;
        private final Class<T> claseValor;
        private final Map<CatalogoClave<T>, Inscripcion<T>> porClave;
        private final List<Inscripcion<T>> porNumerico;
        private final Map<CatalogoClave<T>, T> mapaValores;

        Snapshot(Identifier reestroId, Class<T> claseValor,
                 Map<CatalogoClave<T>, Inscripcion<T>> porClave,
                 List<Inscripcion<T>> porNumerico,
                 Map<CatalogoClave<T>, T> mapaValores) {
            this.reestroId   = reestroId;
            this.claseValor  = claseValor;
            this.porClave    = porClave;
            this.porNumerico = porNumerico;
            this.mapaValores = mapaValores;
        }

        @Override public Identifier reestroId()    { return reestroId; }
        @Override public Class<T>   claseValor()   { return claseValor; }
        @Override public boolean    congelado()    { return true; }
        @Override public int        tamanio()      { return porClave.size(); }
        @Override public boolean contiene(CatalogoClave<T> c) { return porClave.containsKey(c); }
        @Override public Optional<T> obtener(CatalogoClave<T> c) {
            return Optional.ofNullable(mapaValores.get(c));
        }
        @Override public Optional<T> obtener(int n) {
            if (n < 0 || n >= porNumerico.size()) return Optional.empty();
            return Optional.ofNullable(mapaValores.get(porNumerico.get(n).clave()));
        }
        @Override public Optional<Integer> numericoDe(CatalogoClave<T> c) {
            Inscripcion<T> i = porClave.get(c);
            return i == null ? Optional.empty() : Optional.of(i.numerico());
        }
        @Override public Collection<CatalogoClave<T>> claves() { return porClave.keySet(); }
        @Override public Collection<T> valores() { return mapaValores.values(); }
        @Override public Collection<Inscripcion<T>> inscripciones() { return porClave.values(); }
    }
}

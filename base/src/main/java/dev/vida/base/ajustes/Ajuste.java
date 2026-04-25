/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.core.ApiStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Описание одной настройки: путь, тип, умолчание, валидатор.
 *
 * <p>Экземпляры создаются через типобезопасные фабрики:
 * {@link #entero}, {@link #cadena}, {@link #logico}, {@link #flotante},
 * либо через {@link #tipo} для произвольного типа.
 *
 * @param <T> тип значения
 */
@ApiStatus.Stable
public final class Ajuste<T> {

    private final String ruta;
    private final Class<T> clase;
    private final T defecto;
    private final Validador<T> validador;
    private final String descripcion;
    private final boolean sincronizar;

    private Ajuste(Builder<T> b) {
        this.ruta        = Objects.requireNonNull(b.ruta, "ruta");
        this.clase       = Objects.requireNonNull(b.clase, "clase");
        this.defecto     = Objects.requireNonNull(b.defecto, "defecto");
        this.validador   = b.validador;
        this.descripcion = b.descripcion == null ? "" : b.descripcion;
        this.sincronizar = b.sincronizar;
    }

    public String ruta()            { return ruta; }
    public Class<T> clase()         { return clase; }
    public T defecto()              { return defecto; }
    public Validador<T> validador() { return validador; }
    public String descripcion()     { return descripcion; }

    /**
     * Если {@code true}, значение может входить в серверный снимок для клиента
     * (wire-тип в модуле {@code vida-red}: {@code PaqueteAjustesSincronizacionServidor}).
     */
    public boolean sincronizarConServidor() {
        return sincronizar;
    }

    /** Проверяет значение. */
    public Optional<String> validar(T valor) {
        return validador.validar(valor);
    }

    // ================================================================
    //                         Builders
    // ================================================================

    public static Builder<Integer> entero(String ruta, int defecto) {
        return new Builder<>(ruta, Integer.class, defecto);
    }

    public static Builder<Long> largo(String ruta, long defecto) {
        return new Builder<>(ruta, Long.class, defecto);
    }

    public static Builder<Double> flotante(String ruta, double defecto) {
        return new Builder<>(ruta, Double.class, defecto);
    }

    public static Builder<Boolean> logico(String ruta, boolean defecto) {
        return new Builder<>(ruta, Boolean.class, defecto);
    }

    public static Builder<String> cadena(String ruta, String defecto) {
        return new Builder<>(ruta, String.class, defecto);
    }

    public static <T> Builder<T> tipo(String ruta, Class<T> clase, T defecto) {
        return new Builder<>(ruta, clase, defecto);
    }

    /** Fluent builder. */
    public static final class Builder<T> {
        private final String ruta;
        private final Class<T> clase;
        private final T defecto;
        private Validador<T> validador = Validador.ninguno();
        private String descripcion;
        private boolean sincronizar;

        private Builder(String ruta, Class<T> clase, T defecto) {
            this.ruta = Objects.requireNonNull(ruta, "ruta");
            this.clase = Objects.requireNonNull(clase, "clase");
            this.defecto = Objects.requireNonNull(defecto, "defecto");
        }

        public Builder<T> descripcion(String d) { this.descripcion = d; return this; }

        public Builder<T> validador(Validador<T> v) {
            this.validador = this.validador.y(Objects.requireNonNull(v, "v"));
            return this;
        }

        /** Ограничение сверху для {@link Comparable}-типов. */
        public Builder<T> max(T max) {
            Objects.requireNonNull(max, "max");
            return validador(v -> compara(v, max) > 0
                    ? Optional.of(ruta + " = " + v + " exceeds max " + max)
                    : Optional.empty());
        }

        /** Ограничение снизу для {@link Comparable}-типов. */
        public Builder<T> min(T min) {
            Objects.requireNonNull(min, "min");
            return validador(v -> compara(v, min) < 0
                    ? Optional.of(ruta + " = " + v + " below min " + min)
                    : Optional.empty());
        }

        /** Преобразование-проверка: значение -> возможная ошибка. */
        public Builder<T> verificar(Function<T, Optional<String>> fn) {
            return validador(fn::apply);
        }

        /**
         * Помечает настройку для включения в серверный снимок, пересылаемый клиенту
         * (wire-тип в модуле {@code vida-red}: {@code PaqueteAjustesSincronizacionServidor}).
         */
        public Builder<T> sincronizar() {
            this.sincronizar = true;
            return this;
        }

        public Ajuste<T> build() {
            Ajuste<T> a = new Ajuste<>(this);
            if (sincronizar) {
                AjustesSincronizacionCatalogo.registrar(a);
            }
            return a;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static int compara(Object a, Object b) {
            if (a instanceof Comparable c && b.getClass().isInstance(a)) {
                return c.compareTo(b);
            }
            throw new IllegalStateException(
                    "cannot compare " + a + " and " + b + ": types not Comparable-compatible");
        }
    }

    @Override
    public String toString() {
        return "Ajuste(" + ruta + " : " + clase.getSimpleName() + " = " + defecto + ")";
    }
}

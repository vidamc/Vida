/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Неизменяемое описание физических, игровых и звуковых свойств блока.
 *
 * <p>Собирается через fluent-билдер {@link #con()}. Все значения валидируются
 * в момент {@link Constructor#construir()}; свойства, зависящие друг от друга
 * (например, {@code resistenciaExplosion} не меньше нуля при наличии
 * {@link MaterialBloque#inflamable}), проверяются здесь же. Валидация
 * гарантирует, что в runtime ни один блок не имеет «битых» значений.
 *
 * <p>Дизайн: поля — record, билдер — отдельный класс, чтобы иммутабельность
 * не ломалась и не нужно было делать copy-with-setters вручную.
 */
@ApiStatus.Stable
public record PropiedadesBloque(
        MaterialBloque material,
        float dureza,
        float resistenciaExplosion,
        int luzEmitida,
        int opacidad,
        float friccion,
        float velocidadSalto,
        float velocidadCaminar,
        boolean requiereHerramienta,
        NivelHerramienta nivelMinimo,
        Set<TipoHerramienta> herramientas,
        boolean replaceable,
        boolean ticking,
        boolean soporteGravedad,
        FormaColision forma,
        SonidoBloque sonido) {

    /** Компактная валидация в канонической форме. */
    public PropiedadesBloque {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(nivelMinimo, "nivelMinimo");
        Objects.requireNonNull(herramientas, "herramientas");
        Objects.requireNonNull(forma, "forma");
        Objects.requireNonNull(sonido, "sonido");
        if (dureza < -1f) {
            throw new IllegalArgumentException("dureza < -1 (bedrock=-1 допускается): " + dureza);
        }
        if (resistenciaExplosion < 0f) {
            throw new IllegalArgumentException("resistenciaExplosion < 0: " + resistenciaExplosion);
        }
        if (luzEmitida < 0 || luzEmitida > 15) {
            throw new IllegalArgumentException("luzEmitida вне [0..15]: " + luzEmitida);
        }
        if (opacidad < 0 || opacidad > 15) {
            throw new IllegalArgumentException("opacidad вне [0..15]: " + opacidad);
        }
        if (friccion < 0f || friccion > 1f) {
            throw new IllegalArgumentException("friccion вне [0..1]: " + friccion);
        }
        if (velocidadSalto < 0f) {
            throw new IllegalArgumentException("velocidadSalto < 0: " + velocidadSalto);
        }
        if (velocidadCaminar < 0f) {
            throw new IllegalArgumentException("velocidadCaminar < 0: " + velocidadCaminar);
        }
        herramientas = Set.copyOf(herramientas);
        if (material.liquido() && !replaceable) {
            // Жидкости всегда replaceable (через них можно поставить блок).
            throw new IllegalArgumentException(
                    "material " + material + " требует replaceable=true");
        }
    }

    /** Быстрый индикатор «блок светится и надо пересчитать lighting при изменении». */
    public boolean esFuenteLuz() { return luzEmitida > 0; }

    /** Неразрушимый — полный аналог bedrock. */
    public boolean esIndestructible() {
        return dureza < 0f;
    }

    /** Начать построение. */
    public static Constructor con() {
        return new Constructor();
    }

    /** Построить с дефолтными значениями и материалом. */
    public static Constructor con(MaterialBloque material) {
        return new Constructor().material(material);
    }

    // ==================================================================
    //                             Builder
    // ==================================================================

    /**
     * Fluent-билдер.
     *
     * <p>Билдер проверяет тривиальные инварианты в сеттерах (например,
     * диапазон [0..15] для {@code luzEmitida}) — это даёт мгновенный feedback
     * в IDE; финальная перекрёстная валидация живёт в конструкторе
     * {@link PropiedadesBloque}.
     */
    public static final class Constructor {

        private MaterialBloque material = MaterialBloque.GENERICO;
        private float dureza = 1.0f;
        private float resistenciaExplosion = 1.0f;
        private int luzEmitida = 0;
        private int opacidad = 15;
        private float friccion = 0.6f;
        private float velocidadSalto = 1.0f;
        private float velocidadCaminar = 1.0f;
        private boolean requiereHerramienta = false;
        private NivelHerramienta nivelMinimo = NivelHerramienta.NINGUNO;
        private final Set<TipoHerramienta> herramientas = EnumSet.noneOf(TipoHerramienta.class);
        private boolean replaceable = false;
        private boolean ticking = false;
        private boolean soporteGravedad = false;
        private FormaColision forma = FormaColision.completo();
        private SonidoBloque sonido = SonidoBloque.piedra();

        Constructor() {}

        public Constructor material(MaterialBloque m) {
            this.material = Objects.requireNonNull(m, "material");
            // Согласованные дефолты под конкретный материал, если пользователь их не переопределит.
            if (m == MaterialBloque.LIQUIDO) {
                this.replaceable = true;
                this.opacidad = 1;
                this.forma = FormaColision.vacio();
            } else if (m == MaterialBloque.AIRE || m == MaterialBloque.EFIMERO) {
                this.replaceable = true;
                this.opacidad = 0;
                this.forma = FormaColision.vacio();
            } else if (m == MaterialBloque.CRISTAL || m == MaterialBloque.PLANTA) {
                this.opacidad = 0;
            }
            // Звук по материалу
            this.sonido = switch (m) {
                case MADERA, TELA -> SonidoBloque.madera();
                case METAL -> SonidoBloque.metal();
                case PLANTA -> SonidoBloque.hierba();
                case CRISTAL -> SonidoBloque.cristal();
                case TIERRA -> SonidoBloque.arena();
                default -> SonidoBloque.piedra();
            };
            return this;
        }

        public Constructor dureza(float v) {
            if (v < -1f) throw new IllegalArgumentException("dureza < -1");
            this.dureza = v;
            return this;
        }

        public Constructor resistenciaExplosion(float v) {
            if (v < 0f) throw new IllegalArgumentException("resistenciaExplosion < 0");
            this.resistenciaExplosion = v;
            return this;
        }

        public Constructor indestructible() {
            this.dureza = -1f;
            this.resistenciaExplosion = 3_600_000f;
            return this;
        }

        public Constructor luzEmitida(int v) {
            if (v < 0 || v > 15) throw new IllegalArgumentException("luzEmitida вне [0..15]");
            this.luzEmitida = v;
            return this;
        }

        public Constructor opacidad(int v) {
            if (v < 0 || v > 15) throw new IllegalArgumentException("opacidad вне [0..15]");
            this.opacidad = v;
            return this;
        }

        public Constructor friccion(float v) {
            if (v < 0f || v > 1f) throw new IllegalArgumentException("friccion вне [0..1]");
            this.friccion = v;
            return this;
        }

        public Constructor velocidadSalto(float v) {
            if (v < 0f) throw new IllegalArgumentException("velocidadSalto < 0");
            this.velocidadSalto = v;
            return this;
        }

        public Constructor velocidadCaminar(float v) {
            if (v < 0f) throw new IllegalArgumentException("velocidadCaminar < 0");
            this.velocidadCaminar = v;
            return this;
        }

        public Constructor requiereHerramienta(boolean v) {
            this.requiereHerramienta = v;
            return this;
        }

        public Constructor nivelMinimo(NivelHerramienta n) {
            this.nivelMinimo = Objects.requireNonNull(n, "nivelMinimo");
            if (n != NivelHerramienta.NINGUNO) this.requiereHerramienta = true;
            return this;
        }

        public Constructor herramienta(TipoHerramienta t) {
            herramientas.add(Objects.requireNonNull(t, "tipo"));
            return this;
        }

        public Constructor herramientas(TipoHerramienta... ts) {
            Objects.requireNonNull(ts, "ts");
            for (TipoHerramienta t : ts) {
                herramientas.add(Objects.requireNonNull(t, "tipo"));
            }
            return this;
        }

        public Constructor replaceable(boolean v) {
            this.replaceable = v;
            return this;
        }

        public Constructor ticking(boolean v) {
            this.ticking = v;
            return this;
        }

        public Constructor soporteGravedad(boolean v) {
            this.soporteGravedad = v;
            return this;
        }

        public Constructor forma(FormaColision f) {
            this.forma = Objects.requireNonNull(f, "forma");
            return this;
        }

        public Constructor sonido(SonidoBloque s) {
            this.sonido = Objects.requireNonNull(s, "sonido");
            return this;
        }

        /** Собирает иммутабельные свойства. */
        public PropiedadesBloque construir() {
            return new PropiedadesBloque(
                    material, dureza, resistenciaExplosion, luzEmitida, opacidad,
                    friccion, velocidadSalto, velocidadCaminar,
                    requiereHerramienta, nivelMinimo, herramientas,
                    replaceable, ticking, soporteGravedad, forma, sonido);
        }
    }
}

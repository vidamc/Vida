/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Звуковой профиль блока: набор из шести событий (break, step, place, hit, fall,
 * ambient) плюс громкость/питч.
 *
 * <p>Идентификаторы событий — абстрактные {@link Identifier}; связь с
 * vanilla {@code SoundEvent}-реестром делается на стороне моста
 * (будущий {@code vida-render}/{@code vida-mundo}).
 */
@ApiStatus.Preview("bloque")
public record SonidoBloque(
        Identifier romper,
        Identifier pisar,
        Identifier colocar,
        Identifier golpear,
        Identifier caer,
        Identifier ambiente,
        float volumen,
        float tono) {

    /** Конструктор-валидатор. */
    public SonidoBloque {
        Objects.requireNonNull(romper, "romper");
        Objects.requireNonNull(pisar, "pisar");
        Objects.requireNonNull(colocar, "colocar");
        Objects.requireNonNull(golpear, "golpear");
        Objects.requireNonNull(caer, "caer");
        Objects.requireNonNull(ambiente, "ambiente");
        if (volumen < 0f) {
            throw new IllegalArgumentException("volumen < 0: " + volumen);
        }
        if (tono <= 0f) {
            throw new IllegalArgumentException("tono <= 0: " + tono);
        }
    }

    /**
     * Фабрика «монозвук»: все шесть событий получают один и тот же id
     * с суффиксами {@code .break}, {@code .step} и т.п.
     */
    public static SonidoBloque uniforme(Identifier base, float volumen, float tono) {
        Objects.requireNonNull(base, "base");
        return new SonidoBloque(
                sufijo(base, "break"),
                sufijo(base, "step"),
                sufijo(base, "place"),
                sufijo(base, "hit"),
                sufijo(base, "fall"),
                sufijo(base, "ambient"),
                volumen, tono);
    }

    /** Фабрика с громкостью 1.0 и питчем 1.0. */
    public static SonidoBloque uniforme(Identifier base) {
        return uniforme(base, 1.0f, 1.0f);
    }

    private static Identifier sufijo(Identifier base, String suf) {
        return Identifier.of(base.namespace(), base.path() + "." + suf);
    }

    // ------------------------------------------------------------------
    //  Стандартные пресеты — упрощают регистрацию типичных материалов.
    // ------------------------------------------------------------------

    /** Камень vanilla. */
    public static SonidoBloque piedra() {
        return uniforme(Identifier.of("minecraft", "block.stone"));
    }

    /** Дерево vanilla. */
    public static SonidoBloque madera() {
        return uniforme(Identifier.of("minecraft", "block.wood"));
    }

    /** Металл vanilla. */
    public static SonidoBloque metal() {
        return uniforme(Identifier.of("minecraft", "block.metal"));
    }

    /** Трава / листва. */
    public static SonidoBloque hierba() {
        return uniforme(Identifier.of("minecraft", "block.grass"));
    }

    /** Стекло. */
    public static SonidoBloque cristal() {
        return uniforme(Identifier.of("minecraft", "block.glass"));
    }

    /** Песок. */
    public static SonidoBloque arena() {
        return uniforme(Identifier.of("minecraft", "block.sand"));
    }
}

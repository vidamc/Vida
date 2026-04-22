/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.bloque.TipoHerramienta;
import dev.vida.core.ApiStatus;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Инструмент: связь {@link TipoHerramienta} (какие блоки это ломает
 * эффективно) + {@link Material} (какой уровень, прочность, скорость).
 *
 * <p>Обычный предмет-инструмент (кирка, топор, меч) в модах 1.21.1
 * собирается одной строкой: {@code new Herramienta(Material.HIERRO, Set.of(
 * TipoHerramienta.PICO)), dano=1.0f, velAttack=1.2f}. Конкретная реализация
 * «правильного mining-поведения» лежит в будущем {@code vida-mundo}.
 */
@ApiStatus.Preview("objeto")
public record Herramienta(
        Material material,
        Set<TipoHerramienta> tipos,
        float danoAtaque,
        float velocidadAtaque) {

    public Herramienta {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(tipos, "tipos");
        if (tipos.isEmpty()) {
            throw new IllegalArgumentException("tipos пуст — инструмент без категорий");
        }
        tipos = Set.copyOf(tipos);
        if (danoAtaque < 0f) {
            throw new IllegalArgumentException("danoAtaque < 0: " + danoAtaque);
        }
        if (velocidadAtaque <= 0f) {
            throw new IllegalArgumentException("velocidadAtaque <= 0: " + velocidadAtaque);
        }
    }

    /** Обычная кирка данного материала. */
    public static Herramienta pico(Material m) {
        return new Herramienta(m, EnumSet.of(TipoHerramienta.PICO),
                1.0f + m.danoBase(), 1.2f);
    }

    /** Обычный топор (большой урон, медленный). */
    public static Herramienta hacha(Material m) {
        return new Herramienta(m, EnumSet.of(TipoHerramienta.HACHA),
                5.0f + m.danoBase(), 0.9f);
    }

    /** Обычная лопата. */
    public static Herramienta pala(Material m) {
        return new Herramienta(m, EnumSet.of(TipoHerramienta.PALA),
                1.5f + m.danoBase(), 1.0f);
    }

    /** Обычная мотыга. */
    public static Herramienta azada(Material m) {
        return new Herramienta(m, EnumSet.of(TipoHerramienta.AZADA),
                0.0f, 1.0f);
    }

    /** Обычный меч. */
    public static Herramienta espada(Material m) {
        return new Herramienta(m, EnumSet.of(TipoHerramienta.ESPADA),
                3.0f + m.danoBase(), 1.6f);
    }
}

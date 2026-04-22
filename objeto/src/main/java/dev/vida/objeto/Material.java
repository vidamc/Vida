/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.bloque.NivelHerramienta;
import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Материал инструмента/оружия: база под tier'ы 1.21.1 (wood / stone / iron /
 * gold / diamond / netherite) и под пользовательские материалы.
 *
 * <p>Материал задаёт: (a) уровень добычи ({@link NivelHerramienta}), (b)
 * прочность, (c) скорость добычи, (d) базовый урон (для мечей) и (e) бонус
 * ко времени действия зачарований.
 *
 * <p>Четыре стандартных материала доступны как константы; пользовательские
 * создаются через {@link #personalizado(String, NivelHerramienta, int, float, float, int)}.
 */
@ApiStatus.Preview("objeto")
public record Material(
        String nombre,
        NivelHerramienta nivelAddicion,
        int durabilidad,
        float velocidadAccion,
        float danoBase,
        int tasaEncantamiento) {

    public Material {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(nivelAddicion, "nivelAddicion");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("nombre пустой");
        }
        if (durabilidad <= 0) {
            throw new IllegalArgumentException("durabilidad <= 0: " + durabilidad);
        }
        if (velocidadAccion <= 0f) {
            throw new IllegalArgumentException("velocidadAccion <= 0: " + velocidadAccion);
        }
        if (danoBase < 0f) {
            throw new IllegalArgumentException("danoBase < 0: " + danoBase);
        }
        if (tasaEncantamiento < 0) {
            throw new IllegalArgumentException("tasaEncantamiento < 0: " + tasaEncantamiento);
        }
    }

    /** Деревянный tier: 1.21.1 {@code Tiers.WOOD}. */
    public static final Material MADERA =
            new Material("madera", NivelHerramienta.MADERA, 59, 2.0f, 0.0f, 15);

    /** Каменный. */
    public static final Material PIEDRA =
            new Material("piedra", NivelHerramienta.PIEDRA, 131, 4.0f, 1.0f, 5);

    /** Железный. */
    public static final Material HIERRO =
            new Material("hierro", NivelHerramienta.HIERRO, 250, 6.0f, 2.0f, 14);

    /** Золотой: низкая прочность, но высокая encanting-rate. */
    public static final Material ORO =
            new Material("oro", NivelHerramienta.MADERA, 32, 12.0f, 0.0f, 22);

    /** Алмазный. */
    public static final Material DIAMANTE =
            new Material("diamante", NivelHerramienta.DIAMANTE, 1561, 8.0f, 3.0f, 10);

    /** Незеритовый. */
    public static final Material NETHERITA =
            new Material("netherita", NivelHerramienta.NETHERITA, 2031, 9.0f, 4.0f, 15);

    /** Фабрика для пользовательских материалов. */
    public static Material personalizado(String nombre, NivelHerramienta nivel,
                                          int durabilidad, float velocidad, float dano, int encant) {
        return new Material(nombre, nivel, durabilidad, velocidad, dano, encant);
    }
}

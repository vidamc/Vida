/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad;

import dev.vida.core.ApiStatus;

/**
 * Потокобезопасный кэш текущего значения насыщения игрока.
 *
 * <p>Значение обновляется инжектором {@code FoodDataSaciedadMorph} каждый
 * раз, когда vanilla вызывает {@code FoodData#tick(Player)}. Рендерер HUD
 * читает кэш без аллокаций через {@link #leer()}.
 *
 * <h2>Диапазон значений</h2>
 * Vanilla хранит насыщение от {@code 0.0f} до {@code 20.0f} (не превышает
 * текущий уровень голода, то есть тоже {@code 0..20}).
 */
@ApiStatus.Internal
public final class SaciedadCache {

    /** Текущий уровень насыщения, диапазон [0.0, 20.0]. */
    private static volatile float saturation = 0.0f;

    private SaciedadCache() {}

    /**
     * Обновляет кэш. Вызывается только из {@code FoodDataSaciedadMorph}.
     *
     * @param valor новое значение насыщения
     */
    public static void actualizar(float valor) {
        saturation = valor;
    }

    /**
     * Возвращает текущее значение насыщения. Безаллокационный горячий путь.
     *
     * @return насыщение в диапазоне {@code [0.0, 20.0]}
     */
    public static float leer() {
        return saturation;
    }
}

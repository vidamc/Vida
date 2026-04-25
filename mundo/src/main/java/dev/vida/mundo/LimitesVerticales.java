/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Диапазон допустимых координат блока по оси Y (включительно, {@code minY} .. {@code maxY}).
 *
 * <p>Фабрики {@link #overworldVanilla121()}, {@link #netherVanilla121()} и {@link #endVanilla121()}
 * выровнены по типичным пределам генерации Minecraft Java <strong>1.21.x</strong> для встроенных
 * измерений. Пользовательские измерения и datapack-измерения могут отличаться; рантайм-мост может
 * подставлять фактические значения через реализацию {@link Mundo}.
 */
@ApiStatus.Stable
public record LimitesVerticales(int minY, int maxY) {

    public LimitesVerticales {
        if (minY > maxY) {
            throw new IllegalArgumentException("minY > maxY: " + minY + " > " + maxY);
        }
    }

    /**
     * Overworld / The End: минимальная высота постройки и верхняя включительная координата в стиле 1.21.x.
     */
    public static LimitesVerticales overworldVanilla121() {
        return new LimitesVerticales(-64, 319);
    }

    /**
     * Нижний мир: колонка 0..255 (256 блоков), как в типичном профиле Vanilla 1.21.x.
     */
    public static LimitesVerticales netherVanilla121() {
        return new LimitesVerticales(0, 255);
    }

    /**
     * Край: те же вертикальные границы, что и у обычного мира в линии 1.21.x для стандартного профиля.
     */
    public static LimitesVerticales endVanilla121() {
        return overworldVanilla121();
    }

    /**
     * Произвольный диапазон (например, из моста к {@code LevelHeightAccessor}).
     */
    public static LimitesVerticales de(int minY, int maxY) {
        return new LimitesVerticales(minY, maxY);
    }

    /** Проверка одной координаты {@code y}. */
    public boolean contiene(int y) {
        return y >= minY && y <= maxY;
    }

    /** Удобный shortcut: берётся только компонента {@link Coordenada#y}. */
    public boolean contiene(Coordenada coordenada) {
        Objects.requireNonNull(coordenada, "coordenada");
        return contiene(coordenada.y());
    }

    /** Число целых высот в диапазоне ({@code maxY - minY + 1}). */
    public int alturaSpan() {
        return maxY - minY + 1;
    }
}

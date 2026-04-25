/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.core.ApiStatus;

/**
 * Абстракция рисования прямоугольников на HUD.
 *
 * <p>Инстанс предоставляет игровая система рендеринга и передаётся через
 * {@link LatidoRenderHud}. Абстракция позволяет тестировать HUD-рендереры
 * без зависимости от OpenGL.
 *
 * <p>Все координаты в экранных пикселях (origin — верхний левый угол).
 * Цвет передаётся в формате ARGB (α в старшем байте).
 *
 * <h2>Контракт потокобезопасности</h2>
 * Экземпляр безопасно использовать только на главном рендер-потоке, в котором
 * было испущено событие.
 */
@ApiStatus.Stable
@FunctionalInterface
public interface PintorHud {

    /**
     * Рисует строку текста шрифтом HUD. Реализация по умолчанию — no-op (тесты/заглушки);
     * на клиенте инжектируется мост {@code dev.vida.platform} (см. {@code GuiGraphics}
     * + {@code drawString} через {@code MethodHandle}).
     *
     * @param x         левый край базовой линии (px)
     * @param y         верхний край строки (px), как в типичном {@code drawString}
     * @param texto     текст; при {@code null} вызов игнорируется
     * @param colorArgb цвет в ARGB
     */
    default void dibujarCadena(int x, int y, String texto, int colorArgb) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
    }

    /**
     * Рисует залитый прямоугольник.
     *
     * @param x        левый край (px)
     * @param y        верхний край (px)
     * @param ancho    ширина (px); при {@code ≤ 0} вызов игнорируется
     * @param alto     высота (px); при {@code ≤ 0} вызов игнорируется
     * @param colorArgb цвет в ARGB (например {@code 0xFFFF8000})
     */
    void dibujarRectangulo(int x, int y, int ancho, int alto, int colorArgb);
}

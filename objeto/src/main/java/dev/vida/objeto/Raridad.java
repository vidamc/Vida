/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.core.ApiStatus;

/**
 * Редкость предмета — определяет цвет имени и важность UI-подсветки.
 *
 * <p>Соответствует vanilla enum {@code Rarity} в 1.21.1. Vida вводит собственный
 * тип, потому что не хочет тянуть прямую зависимость от Minecraft-классов
 * в side-agnostic модулях. Интеграция с клиентом — в будущем {@code vida-render}.
 */
@ApiStatus.Preview("objeto")
public enum Raridad {
    /** Белое имя, обычный предмет. */
    COMUN(0xFFFFFF),
    /** Жёлтое, как зачарованные книги. */
    INFRECUENTE(0xFFFF55),
    /** Голубое, как зачарованный золотой яблок. */
    RARO(0x55FFFF),
    /** Сиреневое, как драконье яйцо. */
    EPICO(0xFF55FF);

    private final int colorRgb;

    Raridad(int colorRgb) {
        this.colorRgb = colorRgb;
    }

    /** Цвет имени в виде RGB (0xRRGGBB). */
    public int colorRgb() { return colorRgb; }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;

/**
 * Вид инструмента, которым эффективно ломается блок.
 *
 * <p>Для блоков с этим полем игрок получит бонус скорости и дроп только
 * при использовании соответствующего инструмента. Несколько типов можно
 * комбинировать через флаги в {@link PropiedadesBloque#herramientas}.
 */
@ApiStatus.Stable
public enum TipoHerramienta {
    /** Рука. Любой блок с {@link NivelHerramienta#NINGUNO}. */
    MANO,
    /** Кирка: камень, руды. */
    PICO,
    /** Топор: дерево, все растения. */
    HACHA,
    /** Лопата: земля, песок, гравий. */
    PALA,
    /** Мотыга: листья, кусты. */
    AZADA,
    /** Ножницы: шерсть, лиана. */
    TIJERAS,
    /** Меч: ткань, паутина. */
    ESPADA
}

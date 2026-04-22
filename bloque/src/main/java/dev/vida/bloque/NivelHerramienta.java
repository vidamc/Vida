/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;

/**
 * Уровень инструмента, необходимый для получения дропа с блока.
 *
 * <p>Соответствует пирамиде 1.21.1: wooden → stone → iron → diamond → netherite.
 * Отдельно {@link #NINGUNO} — блок ломается рукой.
 *
 * <p>Сравнение: {@link #satisfechoPor(NivelHerramienta)} — возвращает
 * {@code true}, если инструмент уровня {@code que} разрешает добычу блока
 * уровня {@code this}. Правило vanilla: инструмент вставляется в монотонно
 * возрастающий ряд, уровни выше перекрывают нижние.
 */
@ApiStatus.Preview("bloque")
public enum NivelHerramienta {

    /** Ломается рукой. */
    NINGUNO(0),
    /** Деревянная кирка/лопата/топор. */
    MADERA(1),
    /** Каменная. */
    PIEDRA(2),
    /** Железная. */
    HIERRO(3),
    /** Алмазная. */
    DIAMANTE(4),
    /** Незеритовая. */
    NETHERITA(5);

    private final int rango;

    NivelHerramienta(int rango) {
        this.rango = rango;
    }

    /** Числовое значение уровня, 0..5. */
    public int rango() { return rango; }

    /** Инструмент уровня {@code que} разрешает добычу блока уровня {@code this}. */
    public boolean satisfechoPor(NivelHerramienta que) {
        return que.rango >= this.rango;
    }
}

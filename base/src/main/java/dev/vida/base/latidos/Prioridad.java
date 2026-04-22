/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;

/**
 * Приоритет подписчика в шине {@link LatidoBus}.
 *
 * <p>Порядок доставки: сначала {@link #URGENTE}, затем {@link #ALTA},
 * {@link #NORMAL}, {@link #BAJA}, в конце — {@link #MONITOR}.
 *
 * <p>{@link #MONITOR} — особый: он получает событие <b>после</b> всех
 * остальных и не имеет права его отменять (если {@link LatidoCancelable#cancelar()}
 * вызывается там, шина игнорирует это и логирует предупреждение). Используется
 * для телеметрии/логирования, которые не должны влиять на поведение.
 */
@ApiStatus.Preview("base")
public enum Prioridad {
    URGENTE(-200),
    ALTA(-100),
    NORMAL(0),
    BAJA(100),
    MONITOR(1000);

    private final int peso;

    Prioridad(int peso) {
        this.peso = peso;
    }

    /** Чем меньше значение, тем раньше доставка. */
    public int peso() {
        return peso;
    }
}

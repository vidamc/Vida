/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import dev.vida.core.ApiStatus;

/**
 * Приоритет задачи в очереди {@link Susurro}.
 *
 * <p>Правила:
 * <ul>
 *   <li>{@link #ALTA} — выполняется перед всеми NORMAL и BAJA;</li>
 *   <li>{@link #NORMAL} — дефолт, подходит для 99% модовых задач;</li>
 *   <li>{@link #BAJA} — откладывается, если пул занят.</li>
 * </ul>
 *
 * <p>Задачи одинакового приоритета обрабатываются FIFO.
 */
@ApiStatus.Stable
public enum Prioridad {
    ALTA(0),
    NORMAL(1),
    BAJA(2);

    private final int peso;

    Prioridad(int peso) {
        this.peso = peso;
    }

    /** Числовой вес: меньше — выше приоритет. */
    public int peso() { return peso; }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;

/**
 * Фаза доставки события. Внутри одной {@link Prioridad} порядок: {@link #ANTES},
 * {@link #PRINCIPAL}, {@link #DESPUES}.
 *
 * <p>{@link #PRINCIPAL} — основная фаза, в ней обычно находятся «рабочие»
 * подписчики. {@link #ANTES} / {@link #DESPUES} — тонкие настройки порядка
 * для совместимости между модами.
 */
@ApiStatus.Preview("base")
public enum Fase {
    ANTES(-1),
    PRINCIPAL(0),
    DESPUES(1);

    private final int peso;

    Fase(int peso) {
        this.peso = peso;
    }

    public int peso() {
        return peso;
    }
}

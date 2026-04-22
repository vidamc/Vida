/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos.eventos;

import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;

/**
 * Серверный тик. Диспатчится ровно один раз на игровой тик (20 раз в
 * секунду при стандартной частоте).
 *
 * @param tickActual текущий номер тика с момента запуска
 * @param mundo      идентификатор мира (может быть {@code null} — для
 *                   «пре-игровых» тиков загрузчика)
 * @param profundidad 0 — корневой серверный тик; 1+ — вложенные (subtick)
 */
@ApiStatus.Preview("base")
public record LatidoPulso(long tickActual, String mundo, int profundidad) {

    public LatidoPulso {
        if (tickActual < 0) throw new IllegalArgumentException("tickActual < 0");
        if (profundidad < 0) throw new IllegalArgumentException("profundidad < 0");
    }

    public static final Latido<LatidoPulso> TIPO =
            Latido.de("vida:pulso", LatidoPulso.class);

    /** Удобный конструктор корневого тика без мира. */
    public static LatidoPulso raiz(long tick) {
        return new LatidoPulso(tick, null, 0);
    }
}

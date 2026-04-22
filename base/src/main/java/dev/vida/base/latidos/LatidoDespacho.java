/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Objects;

/**
 * Итог одной операции {@link LatidoBus#emitir}.
 *
 * @param recibidos количество подписчиков, которым событие было доставлено
 * @param errores   список исключений, пойманных у подписчиков (в порядке
 *                  доставки). Сама шина их не пробрасывает.
 * @param cancelado {@code true}, если хоть один подписчик вызвал
 *                  {@link LatidoCancelable#cancelar()}. Для не-отменяемых
 *                  событий всегда {@code false}.
 */
@ApiStatus.Preview("base")
public record LatidoDespacho(int recibidos, List<Throwable> errores, boolean cancelado) {

    public LatidoDespacho {
        if (recibidos < 0) throw new IllegalArgumentException("recibidos < 0");
        errores = List.copyOf(Objects.requireNonNull(errores, "errores"));
    }

    public boolean tieneErrores() { return !errores.isEmpty(); }

    public static LatidoDespacho vacio() {
        return new LatidoDespacho(0, List.of(), false);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/** Фрагмент крупного пакета (сервер → клиент). См. {@link PaqueteClienteCargaFragmento}. */
@ApiStatus.Stable
public record PaqueteServidorCargaFragmento(
        long sesion,
        int indice,
        int total,
        int crc32,
        int longitudTotal,
        byte[] fragmento) implements PaqueteServidor {

    public PaqueteServidorCargaFragmento {
        if (indice < 0 || total < 1 || indice >= total) {
            throw new IllegalArgumentException("invalid fragment indices");
        }
        if (longitudTotal < 0) {
            throw new IllegalArgumentException("longitudTotal < 0");
        }
        fragmento = Objects.requireNonNull(fragmento, "fragmento").clone();
    }
}

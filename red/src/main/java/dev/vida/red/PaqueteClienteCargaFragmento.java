/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Один фрагмент бинарного payload логического пакета (клиент → сервер).
 *
 * @param sesion        идентификатор сессии сборки (общий для всех фрагментов)
 * @param indice        {@code [0, total)}
 * @param total         число фрагментов
 * @param crc32         CRC32 полного сообщения (до разбиения)
 * @param longitudTotal длина полного сообщения в байтах
 * @param fragmento     кусок байтов
 */
@ApiStatus.Stable
public record PaqueteClienteCargaFragmento(
        long sesion,
        int indice,
        int total,
        int crc32,
        int longitudTotal,
        byte[] fragmento) implements PaqueteCliente {

    public PaqueteClienteCargaFragmento {
        if (indice < 0 || total < 1 || indice >= total) {
            throw new IllegalArgumentException("invalid fragment indices");
        }
        if (longitudTotal < 0) {
            throw new IllegalArgumentException("longitudTotal < 0");
        }
        fragmento = Objects.requireNonNull(fragmento, "fragmento").clone();
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;

/**
 * Кодек wire-сериализации одного типа пакета.
 */
@ApiStatus.Stable
public interface CodecPaquete<T extends Record> {

    byte[] codificar(T paquete);

    T decodificar(byte[] payload);

    /** Límite duro del tamaño del payload en bytes (wire). */
    default int maxCargaBytes() {
        return 1_048_576;
    }
}

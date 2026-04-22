/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;

/**
 * Кодек wire-сериализации одного типа пакета.
 */
@ApiStatus.Preview("red")
public interface CodecPaquete<T extends Record> {

    byte[] codificar(T paquete);

    T decodificar(byte[] payload);
}

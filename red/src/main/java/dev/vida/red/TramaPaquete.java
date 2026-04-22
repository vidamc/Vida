/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Wire-level представление сетевого пакета.
 */
@ApiStatus.Preview("red")
public record TramaPaquete(
        DireccionPaquete direccion,
        String tipoCanonical,
        int versionCodec,
        byte[] payload) {

    public TramaPaquete {
        Objects.requireNonNull(direccion, "direccion");
        Objects.requireNonNull(tipoCanonical, "tipoCanonical");
        Objects.requireNonNull(payload, "payload");
        if (versionCodec < 1) {
            throw new IllegalArgumentException("versionCodec < 1");
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}

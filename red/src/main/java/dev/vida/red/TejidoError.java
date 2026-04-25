/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Типизированные ошибки сетевого канала Vida.
 */
@ApiStatus.Stable
public sealed interface TejidoError {

    record TipoNoRegistrado(String tipoCanonical, DireccionPaquete direccion) implements TejidoError {
        public TipoNoRegistrado {
            Objects.requireNonNull(tipoCanonical, "tipoCanonical");
            Objects.requireNonNull(direccion, "direccion");
        }
    }

    record VersionNoSoportada(String tipoCanonical, int versionSolicitada) implements TejidoError {
        public VersionNoSoportada {
            Objects.requireNonNull(tipoCanonical, "tipoCanonical");
        }
    }

    record BackPressure(int maxCola) implements TejidoError {}

    record PayloadInvalido(String tipoCanonical, String detalle) implements TejidoError {
        public PayloadInvalido {
            Objects.requireNonNull(tipoCanonical, "tipoCanonical");
            Objects.requireNonNull(detalle, "detalle");
        }
    }

    record CargaDemasiadoGrande(String tipoCanonical, int bytes, int maxPermitidos) implements TejidoError {
        public CargaDemasiadoGrande {
            Objects.requireNonNull(tipoCanonical, "tipoCanonical");
        }
    }
}

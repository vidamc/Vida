/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Value-type биома.
 */
@ApiStatus.Preview("mundo")
public record Bioma(
        Identifier id,
        float temperatura,
        float humedad,
        Precipitacion precipitacion) {

    public Bioma {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(precipitacion, "precipitacion");
        if (!Float.isFinite(temperatura)) {
            throw new IllegalArgumentException("temperatura no es finita: " + temperatura);
        }
        if (!Float.isFinite(humedad) || humedad < 0f || humedad > 1f) {
            throw new IllegalArgumentException("humedad вне [0..1]: " + humedad);
        }
    }

    public boolean esFrio() {
        return temperatura <= 0.15f;
    }

    public boolean tienePrecipitacion() {
        return precipitacion != Precipitacion.NINGUNA;
    }

    public enum Precipitacion {
        NINGUNA,
        LLUVIA,
        NIEVE
    }
}

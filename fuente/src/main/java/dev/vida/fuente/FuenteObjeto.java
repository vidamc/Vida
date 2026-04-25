/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Простейшая item-декларация для data-driven прототипа.
 */
@ApiStatus.Stable
public record FuenteObjeto(
        Identifier id,
        String tipo,
        int maxPila) {

    public FuenteObjeto {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        if (tipo.isBlank()) {
            throw new IllegalArgumentException("tipo is blank");
        }
        if (maxPila < 1 || maxPila > 99) {
            throw new IllegalArgumentException("maxPila fuera de rango [1..99]: " + maxPila);
        }
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Простейшая block-декларация для data-driven прототипа.
 */
@ApiStatus.Preview("loader")
public record FuenteBloque(
        Identifier id,
        String material,
        float dureza) {

    public FuenteBloque {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(material, "material");
        if (material.isBlank()) {
            throw new IllegalArgumentException("material is blank");
        }
        if (!Float.isFinite(dureza) || dureza < 0f) {
            throw new IllegalArgumentException("dureza invalida: " + dureza);
        }
    }
}

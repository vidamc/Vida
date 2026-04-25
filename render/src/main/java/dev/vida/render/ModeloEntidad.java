/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Декларативная render-модель сущности.
 */
@ApiStatus.Stable
public record ModeloEntidad(Identifier malla, Identifier texturaPrincipal) {

    public static final Identifier MALLA_SIMPLE = Identifier.of("vida", "entity/simple");
    public static final Identifier TEXTURA_MISSING = ModeloBloque.TEXTURA_MISSING;

    public static final ModeloEntidad POR_DEFECTO =
            new ModeloEntidad(MALLA_SIMPLE, TEXTURA_MISSING);

    public ModeloEntidad {
        Objects.requireNonNull(malla, "malla");
        Objects.requireNonNull(texturaPrincipal, "texturaPrincipal");
    }

    public static ModeloEntidad simple(Identifier texturaPrincipal) {
        return new ModeloEntidad(MALLA_SIMPLE, texturaPrincipal);
    }
}

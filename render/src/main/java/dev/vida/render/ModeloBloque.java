/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Декларативная render-модель блока.
 */
@ApiStatus.Preview("render")
public record ModeloBloque(Identifier geometria, Identifier texturaPrincipal) {

    public static final Identifier GEOMETRIA_CUBO = Identifier.of("vida", "cube");
    public static final Identifier TEXTURA_MISSING = Identifier.of("vida", "missing_texture");

    public static final ModeloBloque POR_DEFECTO =
            new ModeloBloque(GEOMETRIA_CUBO, TEXTURA_MISSING);

    public ModeloBloque {
        Objects.requireNonNull(geometria, "geometria");
        Objects.requireNonNull(texturaPrincipal, "texturaPrincipal");
    }

    public static ModeloBloque cubo(Identifier texturaPrincipal) {
        return new ModeloBloque(GEOMETRIA_CUBO, texturaPrincipal);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.registro;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Тег предметов. Аналог {@link dev.vida.bloque.registro.EtiquetaBloque}
 * для предметов; типичные vanilla-эквиваленты — {@code #minecraft:wooden_tools},
 * {@code #minecraft:planks_item}.
 */
@ApiStatus.Stable
public record EtiquetaObjeto(Identifier id) {

    public EtiquetaObjeto {
        Objects.requireNonNull(id, "id");
    }

    public static EtiquetaObjeto de(String id) {
        return new EtiquetaObjeto(Identifier.parse(id));
    }

    public static EtiquetaObjeto de(String namespace, String path) {
        return new EtiquetaObjeto(Identifier.of(namespace, path));
    }
}

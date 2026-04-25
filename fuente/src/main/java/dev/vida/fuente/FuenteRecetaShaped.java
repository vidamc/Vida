/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MVP-shaped recipe для data-driven прототипа.
 */
@ApiStatus.Stable
public record FuenteRecetaShaped(
        Identifier id,
        List<String> patron,
        Map<Character, Identifier> claves,
        Resultado resultado) {

    public FuenteRecetaShaped {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(patron, "patron");
        Objects.requireNonNull(claves, "claves");
        Objects.requireNonNull(resultado, "resultado");
        patron = List.copyOf(patron);
        claves = Map.copyOf(claves);
        if (patron.isEmpty()) {
            throw new IllegalArgumentException("patron vacio");
        }
        int ancho = patron.get(0).length();
        if (ancho == 0 || ancho > 3 || patron.size() > 3) {
            throw new IllegalArgumentException("patron fuera de limites 3x3");
        }
        for (String fila : patron) {
            if (fila.length() != ancho) {
                throw new IllegalArgumentException("filas del patron con diferente ancho");
            }
        }
    }

    @ApiStatus.Stable
    public record Resultado(Identifier id, int cantidad) {
        public Resultado {
            Objects.requireNonNull(id, "id");
            if (cantidad < 1 || cantidad > 64) {
                throw new IllegalArgumentException("cantidad fuera de rango [1..64]");
            }
        }
    }
}

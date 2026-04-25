/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Objects;

/**
 * Итог разбора data-driven контента одного мода.
 */
@ApiStatus.Stable
public record FuenteContenidoMod(
        boolean habilitado,
        String rootDatapack,
        List<FuenteBloque> bloques,
        List<FuenteObjeto> objetos,
        List<FuenteRecetaShaped> recetasShaped,
        List<FuenteLootTable> tablasLoot,
        List<FuenteWorldgenHuella> worldgen) {

    public static final FuenteContenidoMod DESHABILITADO =
            new FuenteContenidoMod(false, "", List.of(), List.of(), List.of(), List.of(), List.of());

    public FuenteContenidoMod {
        Objects.requireNonNull(rootDatapack, "rootDatapack");
        bloques = List.copyOf(Objects.requireNonNull(bloques, "bloques"));
        objetos = List.copyOf(Objects.requireNonNull(objetos, "objetos"));
        recetasShaped = List.copyOf(Objects.requireNonNull(recetasShaped, "recetasShaped"));
        tablasLoot = List.copyOf(Objects.requireNonNull(tablasLoot, "tablasLoot"));
        worldgen = List.copyOf(Objects.requireNonNull(worldgen, "worldgen"));
    }

    public boolean vacio() {
        return bloques.isEmpty()
                && objetos.isEmpty()
                && recetasShaped.isEmpty()
                && tablasLoot.isEmpty()
                && worldgen.isEmpty();
    }
}

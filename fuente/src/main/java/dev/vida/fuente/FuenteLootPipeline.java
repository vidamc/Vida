/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.core.Result;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Связность loot-datapack: все вложенные ссылки {@link FuenteLootTable#tablasReferenciadas()}
 * должны совпасть с другой таблицей в этом же {@link FuenteContenidoMod}.
 */
@ApiStatus.Stable
public final class FuenteLootPipeline {

    private FuenteLootPipeline() {}

    /**
     * Проверка внутренних ссылок между loot JSON одного мода (без игрового registry).
     */
    public static Result<Void, FuenteError> validarReferenciasInternas(FuenteContenidoMod contenido) {
        Objects.requireNonNull(contenido, "contenido");
        if (!contenido.habilitado()) {
            return Result.ok(null);
        }
        Set<Identifier> definidas = new HashSet<>();
        for (FuenteLootTable t : contenido.tablasLoot()) {
            definidas.add(t.id());
        }
        for (FuenteLootTable t : contenido.tablasLoot()) {
            for (Identifier ref : t.tablasReferenciadas()) {
                if (!definidas.contains(ref)) {
                    return Result.err(new FuenteError.TablaLootReferenciaRota(t.id(), ref));
                }
            }
        }
        return Result.ok(null);
    }
}

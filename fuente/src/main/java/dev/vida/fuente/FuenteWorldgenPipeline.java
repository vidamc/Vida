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
 * Согласованность JSON под {@code worldgen/} с тем же datapack-снимком Fuente: для идентификаторов в
 * пространстве имён модуля ({@code manifest.id}) loot и блоки должны присутствовать среди
 * деклараций {@link FuenteLootTable} и {@link FuenteBloque}. Ссылки на пространство {@code minecraft}
 * и другие namespace не проверяются (игровой registry).
 *
 * <p>Переход от проверенного JSON к фактической генерации в мире — задача
 * платформенного моста; см. контракт {@code dev.vida.mundo.worldgen.WorldgenEnlazador}.
 */
@ApiStatus.Stable
public final class FuenteWorldgenPipeline {

    private FuenteWorldgenPipeline() {}

    /**
     * @param namespaceMod обычно {@link dev.vida.manifest.ModManifest#id()}
     */
    public static Result<Void, FuenteError> validarVsDatapack(
            FuenteContenidoMod contenido, String namespaceMod) {
        Objects.requireNonNull(contenido, "contenido");
        Objects.requireNonNull(namespaceMod, "namespaceMod");
        if (!contenido.habilitado()) {
            return Result.ok(null);
        }

        Set<Identifier> tablasDefinidas = new HashSet<>();
        for (FuenteLootTable t : contenido.tablasLoot()) {
            tablasDefinidas.add(t.id());
        }
        Set<Identifier> bloquesDefinidos = new HashSet<>();
        for (FuenteBloque b : contenido.bloques()) {
            bloquesDefinidos.add(b.id());
        }

        for (FuenteWorldgenHuella h : contenido.worldgen()) {
            for (Identifier ref : h.refsLoot()) {
                if (!namespaceMod.equals(ref.namespace())) {
                    continue;
                }
                if (!tablasDefinidas.contains(ref)) {
                    return Result.err(new FuenteError.WorldgenLootReferenciaRota(h.pathZip(), ref));
                }
            }
            for (Identifier ref : h.refsBloque()) {
                if (!namespaceMod.equals(ref.namespace())) {
                    continue;
                }
                if (!bloquesDefinidos.contains(ref)) {
                    return Result.err(new FuenteError.WorldgenBloqueReferenciaRoto(h.pathZip(), ref));
                }
            }
        }
        return Result.ok(null);
    }
}

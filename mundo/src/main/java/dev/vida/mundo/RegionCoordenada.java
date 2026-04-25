/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;

/**
 * Индекс файла региона (область 32×32 чанков, см. {@link #CHUNKS_POR_REGION}),
 * без привязки к vanilla-классам.
 *
 * <p>Используется для ключей кэшей и логики, совместимой с именованием {@code region/r.X.Z.mca}.
 */
@ApiStatus.Stable
public record RegionCoordenada(int regionX, int regionZ) {

    /** Сторона региона в чанках (как в типичном формате {@code .mca}). */
    public static final int CHUNKS_POR_REGION = 32;

    /**
     * Регион, которому принадлежит чанк.
     */
    public static RegionCoordenada desde(ChunkCoordenada chunk) {
        return new RegionCoordenada(
                Math.floorDiv(chunk.chunkX(), CHUNKS_POR_REGION),
                Math.floorDiv(chunk.chunkZ(), CHUNKS_POR_REGION));
    }

    /**
     * Регион для блоковой координаты (через координату чанка).
     */
    public static RegionCoordenada desde(Coordenada bloque) {
        return desde(ChunkCoordenada.desde(bloque));
    }

    /**
     * Упаковка пары координат региона в {@code long} (тот же макет, что у {@link ChunkCoordenada}).
     */
    public long clave() {
        return ChunkCoordenada.empaquetar(regionX, regionZ);
    }
}

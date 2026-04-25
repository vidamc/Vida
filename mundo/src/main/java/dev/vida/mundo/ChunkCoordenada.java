/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;

/**
 * Горизонтальная координата чанка ({@code chunkX}, {@code chunkZ}) без привязки к vanilla-классам.
 *
 * <p>Формат {@link #clave()} совместим с типичным упаковыванием MC: нижние 32 бита —
 * {@code chunkX}, верхние — {@code chunkZ}.
 */
@ApiStatus.Stable
public record ChunkCoordenada(int chunkX, int chunkZ) {

    /**
     * Строит координату чанка из блоковой координаты.
     */
    public static ChunkCoordenada desde(Coordenada coordenada) {
        return new ChunkCoordenada(coordenada.chunkX(), coordenada.chunkZ());
    }

    /**
     * Упаковывает пару чанковых координат в один {@code long} для ключей карт и множеств.
     */
    public static long empaquetar(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFF_FFFFL) | (((long) chunkZ & 0xFFFF_FFFFL) << 32);
    }

    /**
     * Распаковывает {@link #empaquetar(int, int)}.
     */
    public static ChunkCoordenada desempaquetar(long clave) {
        return new ChunkCoordenada((int) clave, (int) (clave >>> 32));
    }

    /**
     * То же, что {@link #empaquetar(int, int)} для компонентов этой записи.
     */
    public long clave() {
        return empaquetar(chunkX, chunkZ);
    }

    /**
     * Регион {@code .mca}, которому принадлежит этот чанк.
     */
    public RegionCoordenada region() {
        return RegionCoordenada.desde(this);
    }
}

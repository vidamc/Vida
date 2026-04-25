/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Трёхмерная координата блока в мире.
 */
@ApiStatus.Stable
public record Coordenada(int x, int y, int z) {

    public Coordenada desplazar(int dx, int dy, int dz) {
        return new Coordenada(x + dx, y + dy, z + dz);
    }

    public int chunkX() {
        return Math.floorDiv(x, 16);
    }

    public int chunkZ() {
        return Math.floorDiv(z, 16);
    }

    /**
     * Горизонтальная координата чанка, которому принадлежит этот блок.
     */
    public ChunkCoordenada chunk() {
        return new ChunkCoordenada(chunkX(), chunkZ());
    }

    /**
     * Регион файла сохранения, содержащий этот блок.
     */
    public RegionCoordenada region() {
        return RegionCoordenada.desde(this);
    }

    public long distanciaCuadrada(Coordenada otra) {
        Objects.requireNonNull(otra, "otra");
        long dx = (long) x - otra.x;
        long dy = (long) y - otra.y;
        long dz = (long) z - otra.z;
        return dx * dx + dy * dy + dz * dz;
    }
}

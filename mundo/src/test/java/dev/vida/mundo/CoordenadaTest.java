/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoordenadaTest {

    @Test
    void desplazar_y_chunk_helpers_funcionan() {
        Coordenada base = new Coordenada(31, 64, -17);

        assertThat(base.desplazar(1, -4, 2)).isEqualTo(new Coordenada(32, 60, -15));
        assertThat(base.chunkX()).isEqualTo(1);
        assertThat(base.chunkZ()).isEqualTo(-2);
        assertThat(base.chunk()).isEqualTo(new ChunkCoordenada(1, -2));
        assertThat(base.region()).isEqualTo(new RegionCoordenada(0, -1));
    }

    @Test
    void distancia_cuadrada_es_estable() {
        Coordenada a = new Coordenada(0, 64, 0);
        Coordenada b = new Coordenada(3, 68, 4);

        assertThat(a.distanciaCuadrada(b)).isEqualTo(41L);
        assertThat(b.distanciaCuadrada(a)).isEqualTo(41L);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegionCoordenadaTest {

    @Test
    void desde_chunk_y_bloque_coinciden() {
        ChunkCoordenada ch = new ChunkCoordenada(31, -5);
        RegionCoordenada r = RegionCoordenada.desde(ch);
        assertThat(r).isEqualTo(new RegionCoordenada(0, -1));

        Coordenada bloque = new Coordenada(500, 64, -80);
        assertThat(RegionCoordenada.desde(bloque)).isEqualTo(ChunkCoordenada.desde(bloque).region());
        assertThat(bloque.region()).isEqualTo(RegionCoordenada.desde(bloque));
    }

    @Test
    void clave_es_empaquetado_region() {
        RegionCoordenada r = new RegionCoordenada(2, -3);
        assertThat(r.clave()).isEqualTo(ChunkCoordenada.empaquetar(2, -3));
    }
}

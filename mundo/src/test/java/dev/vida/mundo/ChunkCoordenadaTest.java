/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChunkCoordenadaTest {

    @Test
    void desde_bloque_y_roundtrip_clave() {
        Coordenada bloque = new Coordenada(31, 64, -17);
        ChunkCoordenada c = ChunkCoordenada.desde(bloque);
        assertThat(c).isEqualTo(new ChunkCoordenada(1, -2));
        assertThat(bloque.chunk()).isEqualTo(c);

        long clave = c.clave();
        assertThat(ChunkCoordenada.desempaquetar(clave)).isEqualTo(c);
        assertThat(ChunkCoordenada.empaquetar(1, -2)).isEqualTo(clave);
    }

    @Test
    void empaquetar_cubre_rango_entero_tipico() {
        assertThat(ChunkCoordenada.desempaquetar(ChunkCoordenada.empaquetar(-1, 0x7fff_ffff)))
                .isEqualTo(new ChunkCoordenada(-1, 0x7fff_ffff));
    }
}

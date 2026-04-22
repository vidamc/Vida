/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

class SonidoBloqueTest {

    @Test
    void uniforme_genera_seis_eventos() {
        SonidoBloque s = SonidoBloque.uniforme(Identifier.of("ejemplo", "arena"));
        assertThat(s.romper().path()).isEqualTo("arena.break");
        assertThat(s.pisar().path()).isEqualTo("arena.step");
        assertThat(s.colocar().path()).isEqualTo("arena.place");
        assertThat(s.golpear().path()).isEqualTo("arena.hit");
        assertThat(s.caer().path()).isEqualTo("arena.fall");
        assertThat(s.ambiente().path()).isEqualTo("arena.ambient");
        assertThat(s.volumen()).isEqualTo(1.0f);
        assertThat(s.tono()).isEqualTo(1.0f);
    }

    @Test
    void presets_comunes_disponibles() {
        assertThat(SonidoBloque.piedra().romper().toString()).isEqualTo("minecraft:block.stone.break");
        assertThat(SonidoBloque.madera().romper().toString()).isEqualTo("minecraft:block.wood.break");
        assertThat(SonidoBloque.metal().romper().toString()).isEqualTo("minecraft:block.metal.break");
        assertThat(SonidoBloque.hierba().romper().toString()).isEqualTo("minecraft:block.grass.break");
        assertThat(SonidoBloque.cristal().romper().toString()).isEqualTo("minecraft:block.glass.break");
        assertThat(SonidoBloque.arena().romper().toString()).isEqualTo("minecraft:block.sand.break");
    }

    @Test
    void volumen_negativo_lanza() {
        Identifier base = Identifier.of("x", "y");
        assertThatThrownBy(() -> SonidoBloque.uniforme(base, -0.1f, 1f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tono_cero_o_negativo_lanza() {
        Identifier base = Identifier.of("x", "y");
        assertThatThrownBy(() -> SonidoBloque.uniforme(base, 1f, 0f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SonidoBloque.uniforme(base, 1f, -1f))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

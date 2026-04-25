/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LimitesVerticalesTest {

    @Test
    void vanilla121_perfiles_y_contiene_coordenada() {
        LimitesVerticales ow = LimitesVerticales.overworldVanilla121();
        assertThat(ow.contiene(-64)).isTrue();
        assertThat(ow.contiene(319)).isTrue();
        assertThat(ow.contiene(320)).isFalse();
        assertThat(ow.alturaSpan()).isEqualTo(384);

        LimitesVerticales ne = LimitesVerticales.netherVanilla121();
        assertThat(ne.contiene(0)).isTrue();
        assertThat(ne.contiene(255)).isTrue();
        assertThat(ne.contiene(-1)).isFalse();

        Coordenada bloque = new Coordenada(0, 100, 0);
        assertThat(ow.contiene(bloque)).isTrue();
    }

    @Test
    void rechaza_min_mayor_que_max() {
        assertThatThrownBy(() -> LimitesVerticales.de(10, 9)).isInstanceOf(IllegalArgumentException.class);
    }
}

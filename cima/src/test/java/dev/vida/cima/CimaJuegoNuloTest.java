/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cima;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CimaJuegoNuloTest {

    @Test
    void nulo_siempre_vacio() {
        assertThat(CimaJuegoNulo.INSTANCIA.vinculado()).isFalse();
        assertThat(CimaJuegoNulo.INSTANCIA.nivelMinecraftVivo()).isEmpty();
        assertThat(CimaJuegoNulo.INSTANCIA.mundoSobreNivelCargado()).isEmpty();
    }

}

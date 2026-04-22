/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class EstadoModTest {

    @Test
    void terminales_son_definitivos() {
        assertThat(EstadoMod.DETENIDO.esTerminal()).isTrue();
        assertThat(EstadoMod.FALLO.esTerminal()).isTrue();
        for (EstadoMod e : EstadoMod.values()) {
            if (e != EstadoMod.DETENIDO && e != EstadoMod.FALLO) {
                assertThat(e.esTerminal()).isFalse();
            }
        }
    }

    @Test
    void transiciones_validas() {
        assertThat(EstadoMod.CARGADO.puedeTransitar(EstadoMod.INICIADO)).isTrue();
        assertThat(EstadoMod.INICIADO.puedeTransitar(EstadoMod.ACTIVO)).isTrue();
        assertThat(EstadoMod.ACTIVO.puedeTransitar(EstadoMod.DETENIENDOSE)).isTrue();
        assertThat(EstadoMod.DETENIENDOSE.puedeTransitar(EstadoMod.DETENIDO)).isTrue();
    }

    @Test
    void transiciones_invalidas() {
        assertThat(EstadoMod.CARGADO.puedeTransitar(EstadoMod.ACTIVO)).isFalse();
        assertThat(EstadoMod.ACTIVO.puedeTransitar(EstadoMod.CARGADO)).isFalse();
        assertThat(EstadoMod.DETENIDO.puedeTransitar(EstadoMod.ACTIVO)).isFalse();
        assertThat(EstadoMod.FALLO.puedeTransitar(EstadoMod.ACTIVO)).isFalse();
    }

    @Test
    void fallo_siempre_достижим_пока_не_terminal() {
        for (EstadoMod e : EstadoMod.values()) {
            boolean esperado = !e.esTerminal();
            assertThat(e.puedeTransitar(EstadoMod.FALLO))
                    .as("fallo reachable from %s", e)
                    .isEqualTo(esperado);
        }
    }
}

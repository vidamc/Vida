/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EtiquetaTest {

    @Test
    void crear_normal() {
        Etiqueta e = Etiqueta.de("mi-mod/ai");
        assertThat(e.valor()).isEqualTo("mi-mod/ai");
    }

    @Test
    void vacia_lanza() {
        assertThatThrownBy(() -> Etiqueta.de(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Etiqueta.de("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void muy_larga_lanza() {
        String s = "a".repeat(129);
        assertThatThrownBy(() -> Etiqueta.de(s))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prioridad_pesos_monotonos() {
        assertThat(Prioridad.ALTA.peso()).isLessThan(Prioridad.NORMAL.peso());
        assertThat(Prioridad.NORMAL.peso()).isLessThan(Prioridad.BAJA.peso());
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaterialBloqueTest {

    @Test
    void aire_vacio_y_traversable() {
        assertThat(MaterialBloque.AIRE.solido()).isFalse();
        assertThat(MaterialBloque.AIRE.traversable()).isTrue();
        assertThat(MaterialBloque.AIRE.permiteLuz()).isTrue();
    }

    @Test
    void metal_solido_no_inflamable() {
        assertThat(MaterialBloque.METAL.solido()).isTrue();
        assertThat(MaterialBloque.METAL.inflamable()).isFalse();
    }

    @Test
    void madera_es_inflamable() {
        assertThat(MaterialBloque.MADERA.inflamable()).isTrue();
    }

    @Test
    void liquido_bloquea_movimiento() {
        assertThat(MaterialBloque.LIQUIDO.liquido()).isTrue();
        assertThat(MaterialBloque.LIQUIDO.bloqueaMovimiento()).isTrue();
    }

    @Test
    void cristal_permite_luz_y_traversable() {
        assertThat(MaterialBloque.CRISTAL.permiteLuz()).isTrue();
        assertThat(MaterialBloque.CRISTAL.traversable()).isTrue();
    }

    @Test
    void planta_inflamable_y_traversable() {
        assertThat(MaterialBloque.PLANTA.inflamable()).isTrue();
        assertThat(MaterialBloque.PLANTA.traversable()).isTrue();
    }
}

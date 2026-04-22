/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FormaColisionTest {

    @Test
    void completo_es_cubo_unitario() {
        FormaColision f = FormaColision.completo();
        assertThat(f.esVacio()).isFalse();
        assertThat(f.cantidadCajas()).isEqualTo(1);
        assertThat(f.volumen()).isEqualTo(1.0);
    }

    @Test
    void vacio_no_tiene_cajas() {
        assertThat(FormaColision.vacio().esVacio()).isTrue();
        assertThat(FormaColision.vacio().exterior()).isNull();
    }

    @Test
    void aabb_basico_calcula_volumen() {
        FormaColision paso = FormaColision.aabb(0, 0, 0, 1, 0.5, 1);
        assertThat(paso.volumen()).isEqualTo(0.5);
    }

    @Test
    void union_acumula_cajas() {
        FormaColision a = FormaColision.aabb(0, 0, 0, 1, 0.5, 1);
        FormaColision b = FormaColision.aabb(0, 0.5, 0, 1, 1.0, 0.5);
        FormaColision escalera = FormaColision.union(a, b);
        assertThat(escalera.cantidadCajas()).isEqualTo(2);
        assertThat(escalera.volumen()).isEqualTo(0.75);
    }

    @Test
    void exterior_cubre_todas_las_cajas() {
        FormaColision t = FormaColision.union(
                FormaColision.aabb(0.1, 0.0, 0.2, 0.4, 0.3, 0.5),
                FormaColision.aabb(0.6, 0.4, 0.5, 0.9, 0.8, 0.7));
        FormaColision.Caja ext = t.exterior();
        assertThat(ext.minX()).isEqualTo(0.1);
        assertThat(ext.minY()).isEqualTo(0.0);
        assertThat(ext.minZ()).isEqualTo(0.2);
        assertThat(ext.maxX()).isEqualTo(0.9);
        assertThat(ext.maxY()).isEqualTo(0.8);
        assertThat(ext.maxZ()).isEqualTo(0.7);
    }

    @Test
    void contiene_punto() {
        FormaColision f = FormaColision.aabb(0, 0, 0, 0.5, 0.5, 0.5);
        assertThat(f.contiene(0.25, 0.25, 0.25)).isTrue();
        assertThat(f.contiene(0.75, 0.5, 0.5)).isFalse();
    }

    @Test
    void caja_invalida_min_mayor_max_lanza() {
        assertThatThrownBy(() -> new FormaColision.Caja(1, 0, 0, 0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("min > max");
    }

    @Test
    void caja_fuera_de_rango_lanza() {
        assertThatThrownBy(() -> new FormaColision.Caja(-2, 0, 0, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("диапазон");
    }

    @Test
    void equals_y_hashcode_estables() {
        FormaColision a = FormaColision.aabb(0, 0, 0, 1, 0.5, 1);
        FormaColision b = FormaColision.aabb(0, 0, 0, 1, 0.5, 1);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}

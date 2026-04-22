/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.bloque.NivelHerramienta;
import dev.vida.bloque.TipoHerramienta;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class HerramientaTest {

    @Test
    void materiales_estandar_creados() {
        assertThat(Material.MADERA.nivelAddicion()).isEqualTo(NivelHerramienta.MADERA);
        assertThat(Material.DIAMANTE.durabilidad()).isEqualTo(1561);
        assertThat(Material.NETHERITA.nivelAddicion()).isEqualTo(NivelHerramienta.NETHERITA);
    }

    @Test
    void material_invalido_lanza() {
        assertThatThrownBy(() -> Material.personalizado("", NivelHerramienta.HIERRO, 100, 4f, 0f, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Material.personalizado("x", NivelHerramienta.HIERRO, 0, 4f, 0f, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Material.personalizado("x", NivelHerramienta.HIERRO, 100, 0f, 0f, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pico_hierro_danos_y_slots() {
        Herramienta h = Herramienta.pico(Material.HIERRO);
        assertThat(h.material()).isEqualTo(Material.HIERRO);
        assertThat(h.tipos()).containsExactly(TipoHerramienta.PICO);
        assertThat(h.danoAtaque()).isEqualTo(1.0f + Material.HIERRO.danoBase());
    }

    @Test
    void espada_neta_fuerte() {
        Herramienta h = Herramienta.espada(Material.NETHERITA);
        assertThat(h.danoAtaque()).isEqualTo(3.0f + 4.0f);
        assertThat(h.tipos()).containsExactly(TipoHerramienta.ESPADA);
    }

    @Test
    void tipos_inmutables() {
        Herramienta h = new Herramienta(Material.HIERRO,
                EnumSet.of(TipoHerramienta.HACHA), 2f, 1f);
        assertThatThrownBy(() -> h.tipos().add(TipoHerramienta.ESPADA))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void tipos_vacios_lanza() {
        assertThatThrownBy(() -> new Herramienta(Material.HIERRO,
                EnumSet.noneOf(TipoHerramienta.class), 2f, 1f))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

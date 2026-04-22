/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PropiedadesBloqueTest {

    @Test
    void dureza_negativa_mayor_que_menos_uno_lanza() {
        assertThatThrownBy(() -> PropiedadesBloque.con().dureza(-5f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bedrock_con_indestructible() {
        PropiedadesBloque p = PropiedadesBloque.con(MaterialBloque.PIEDRA)
                .indestructible()
                .construir();
        assertThat(p.esIndestructible()).isTrue();
        assertThat(p.dureza()).isEqualTo(-1f);
    }

    @Test
    void luz_fuera_de_rango_lanza() {
        assertThatThrownBy(() -> PropiedadesBloque.con().luzEmitida(16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropiedadesBloque.con().luzEmitida(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void liquido_reemplazable_por_defecto() {
        PropiedadesBloque p = PropiedadesBloque.con(MaterialBloque.LIQUIDO).construir();
        assertThat(p.replaceable()).isTrue();
        assertThat(p.forma().esVacio()).isTrue();
    }

    @Test
    void liquido_con_replaceable_false_lanza() {
        assertThatThrownBy(() -> PropiedadesBloque.con(MaterialBloque.LIQUIDO)
                .replaceable(false)
                .construir())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void herramientas_son_inmutables() {
        PropiedadesBloque p = PropiedadesBloque.con(MaterialBloque.PIEDRA)
                .herramientas(TipoHerramienta.PICO, TipoHerramienta.MANO)
                .construir();
        assertThat(p.herramientas()).containsExactlyInAnyOrder(
                TipoHerramienta.PICO, TipoHerramienta.MANO);
        assertThatThrownBy(() -> p.herramientas().add(TipoHerramienta.ESPADA))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nivel_minimo_fuerza_requiere_herramienta() {
        PropiedadesBloque p = PropiedadesBloque.con(MaterialBloque.PIEDRA)
                .nivelMinimo(NivelHerramienta.HIERRO)
                .construir();
        assertThat(p.requiereHerramienta()).isTrue();
        assertThat(p.nivelMinimo()).isEqualTo(NivelHerramienta.HIERRO);
    }

    @Test
    void valores_por_defecto_razonables() {
        PropiedadesBloque p = PropiedadesBloque.con().construir();
        assertThat(p.material()).isEqualTo(MaterialBloque.GENERICO);
        assertThat(p.dureza()).isEqualTo(1.0f);
        assertThat(p.opacidad()).isEqualTo(15);
        assertThat(p.friccion()).isEqualTo(0.6f);
        assertThat(p.requiereHerramienta()).isFalse();
        assertThat(p.nivelMinimo()).isEqualTo(NivelHerramienta.NINGUNO);
    }

    @Test
    void satisfecho_por_nivel_superior() {
        assertThat(NivelHerramienta.HIERRO.satisfechoPor(NivelHerramienta.DIAMANTE)).isTrue();
        assertThat(NivelHerramienta.HIERRO.satisfechoPor(NivelHerramienta.PIEDRA)).isFalse();
    }

    @Test
    void fuente_luz() {
        PropiedadesBloque p = PropiedadesBloque.con(MaterialBloque.PIEDRA)
                .luzEmitida(14)
                .construir();
        assertThat(p.esFuenteLuz()).isTrue();

        PropiedadesBloque sin = PropiedadesBloque.con().construir();
        assertThat(sin.esFuenteLuz()).isFalse();
    }

    @Test
    void sonido_por_material_es_madera_para_madera() {
        PropiedadesBloque p = PropiedadesBloque.con(MaterialBloque.MADERA).construir();
        assertThat(p.sonido()).isEqualTo(SonidoBloque.madera());
    }
}

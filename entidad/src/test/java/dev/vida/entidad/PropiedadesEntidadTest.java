/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.entidad.componentes.ClaveComponenteEntidad;
import dev.vida.entidad.componentes.ComponenteEntidad;
import org.junit.jupiter.api.Test;

class PropiedadesEntidadTest {

    @Test
    void defaults_son_validos() {
        PropiedadesEntidad props = PropiedadesEntidad.con().construir();

        assertThat(props.masa()).isEqualTo(1.0d);
        assertThat(props.hitbox()).isEqualTo(new PropiedadesEntidad.Hitbox(0.6d, 1.8d, 0.6d));
        assertThat(props.tieneIa()).isFalse();
        assertThat(props.componentes().esVacio()).isTrue();
    }

    @Test
    void builder_agrega_grupos_y_componentes() {
        PropiedadesEntidad props = PropiedadesEntidad.con()
                .masa(80.0d)
                .hitbox(0.9d, 1.2d, 0.9d)
                .grupoIa(PropiedadesEntidad.GrupoIa.TERRESTRE)
                .grupoIa(PropiedadesEntidad.GrupoIa.PASIVO)
                .componente(ClaveComponenteEntidad.SALUD, new ComponenteEntidad.Salud(12.0d, 12.0d))
                .construir();

        assertThat(props.gruposIa())
                .containsExactlyInAnyOrder(
                        PropiedadesEntidad.GrupoIa.TERRESTRE,
                        PropiedadesEntidad.GrupoIa.PASIVO);
        assertThat(props.componentes().obtener(ClaveComponenteEntidad.SALUD))
                .hasValueSatisfying(salud -> assertThat(salud.maxima()).isEqualTo(12.0d));
        assertThat(props.volumenHitbox()).isCloseTo(0.972d, org.assertj.core.data.Offset.offset(0.000001d));
    }

    @Test
    void valida_masa_y_hitbox() {
        assertThatThrownBy(() -> PropiedadesEntidad.con().masa(0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("masa");

        assertThatThrownBy(() -> PropiedadesEntidad.con().hitbox(0d, 1d, 1d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancho");
    }
}

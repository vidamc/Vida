/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad.componentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

class ComponenteEntidadTest {

    @Test
    void salud_valida_rangos() {
        assertThatThrownBy(() -> new ComponenteEntidad.Salud(5.0d, 4.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actual");
    }

    @Test
    void mapa_componentes_es_tipado_e_inmutable() {
        MapaComponentesEntidad mapa = MapaComponentesEntidad.con()
                .poner(ClaveComponenteEntidad.SALUD, new ComponenteEntidad.Salud(8.0d, 8.0d))
                .poner(ClaveComponenteEntidad.TABLA_BOTIN,
                        new ComponenteEntidad.TablaBotin(Identifier.of("bosque", "entidades/luci"))
                ).construir();

        assertThat(mapa.contiene(ClaveComponenteEntidad.SALUD)).isTrue();
        assertThat(mapa.obtener(ClaveComponenteEntidad.TABLA_BOTIN))
                .hasValueSatisfying(tabla -> assertThat(tabla.id().toString())
                        .isEqualTo("bosque:entidades/luci"));
        assertThat(mapa.ids()).hasSize(2);
    }

    @Test
    void fusionar_reemplaza_valores_por_clave() {
        MapaComponentesEntidad base = MapaComponentesEntidad.con()
                .poner(ClaveComponenteEntidad.BRILLO, new ComponenteEntidad.Brillo(false))
                .construir();
        MapaComponentesEntidad override = MapaComponentesEntidad.con()
                .poner(ClaveComponenteEntidad.BRILLO, new ComponenteEntidad.Brillo(true))
                .construir();

        assertThat(base.fusionar(override).obtener(ClaveComponenteEntidad.BRILLO))
                .hasValueSatisfying(brillo -> assertThat(brillo.visible()).isTrue());
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.objeto.componentes.ClaveComponente;
import dev.vida.objeto.componentes.Componente;
import org.junit.jupiter.api.Test;

class PropiedadesObjetoTest {

    @Test
    void defaults_razonables() {
        PropiedadesObjeto p = PropiedadesObjeto.con().construir();
        assertThat(p.tipo()).isEqualTo(TipoObjeto.GENERICO);
        assertThat(p.maxPila()).isEqualTo(64);
        assertThat(p.raridad()).isEqualTo(Raridad.COMUN);
        assertThat(p.herramienta()).isEmpty();
        assertThat(p.componentes().esVacio()).isTrue();
    }

    @Test
    void max_pila_fuera_de_rango_lanza() {
        assertThatThrownBy(() -> PropiedadesObjeto.con().maxPila(100).construir())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropiedadesObjeto.con().maxPila(0).construir())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void herramienta_fuerza_tipo_y_pila_1() {
        Herramienta h = Herramienta.pico(Material.HIERRO);
        PropiedadesObjeto p = PropiedadesObjeto.con().herramienta(h).construir();
        assertThat(p.tipo()).isEqualTo(TipoObjeto.HERRAMIENTA);
        assertThat(p.maxPila()).isEqualTo(1);
        assertThat(p.esHerramienta()).isTrue();
        assertThat(p.herramienta()).contains(h);
    }

    @Test
    void tipo_herramienta_sin_herramienta_lanza() {
        assertThatThrownBy(() -> PropiedadesObjeto.con().tipo(TipoObjeto.HERRAMIENTA).construir())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void componentes_conservados_en_construccion() {
        PropiedadesObjeto p = PropiedadesObjeto.con()
                .componente(ClaveComponente.DATOS_MODELO,
                        new Componente.DatosModeloPersonalizados(7))
                .componente(ClaveComponente.COMIDA,
                        new Componente.Comida(4, 1.2f, false, 32))
                .construir();
        assertThat(p.componentes().tamanio()).isEqualTo(2);
        assertThat(p.componentes().obtener(ClaveComponente.DATOS_MODELO))
                .hasValueSatisfying(v -> assertThat(v.valor()).isEqualTo(7));
    }

    @Test
    void arma_fuerza_pila_1() {
        PropiedadesObjeto p = PropiedadesObjeto.con().tipo(TipoObjeto.ARMA).construir();
        assertThat(p.maxPila()).isEqualTo(1);
    }
}

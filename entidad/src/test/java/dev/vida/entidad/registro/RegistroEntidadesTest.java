/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.core.Identifier;
import dev.vida.entidad.Entidad;
import dev.vida.entidad.PropiedadesEntidad;
import dev.vida.entidad.TipoEntidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistroEntidadesTest {

    private CatalogoManejador manejador;
    private RegistroEntidades reg;

    @BeforeEach
    void setUp() {
        manejador = new CatalogoManejador();
        reg = RegistroEntidades.conectar(manejador, "bosque");
    }

    @Test
    void registrar_y_recuperar_por_id() {
        Entidad entidad = entidad("cierva", TipoEntidad.CRIATURA);

        assertThat(reg.registrar(entidad).isOk()).isTrue();
        assertThat(reg.obtener("cierva")).hasValue(entidad);
        assertThat(reg.cantidad()).isEqualTo(1);
    }

    @Test
    void registrar_duplicado_devuelve_error() {
        Entidad entidad = entidad("cierva", TipoEntidad.CRIATURA);

        assertThat(reg.registrar(entidad).isOk()).isTrue();
        assertThat(reg.registrar(entidad).isErr()).isTrue();
    }

    @Test
    void compartido_con_otras_vistas_del_manejador() {
        reg.registrarOExigir(entidad("oso", TipoEntidad.MONSTRUO));

        RegistroEntidades otro = RegistroEntidades.conectar(manejador, "otro");
        assertThat(otro.cantidad()).isEqualTo(1);
        assertThat(otro.obtener(Identifier.of("bosque", "oso"))).isPresent();
    }

    @Test
    void registrar_o_exigir_lanza_en_duplicado() {
        Entidad entidad = entidad("luci", TipoEntidad.AMBIENTAL);

        reg.registrarOExigir(entidad);
        assertThatThrownBy(() -> reg.registrarOExigir(entidad))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void congelar_bloquea_nuevas_registraciones() {
        reg.registrarOExigir(entidad("a", TipoEntidad.UTILIDAD));
        reg.congelar();

        assertThat(reg.congelado()).isTrue();
        assertThat(reg.registrar(entidad("b", TipoEntidad.UTILIDAD)).isErr()).isTrue();
    }

    @Test
    void constante_catalogo_id_es_vida_entidad() {
        assertThat(RegistroEntidades.CATALOGO_ID).isEqualTo(Identifier.of("vida", "entidad"));
    }

    private static Entidad entidad(String path, TipoEntidad tipo) {
        return new Entidad(
                Identifier.of("bosque", path),
                tipo,
                PropiedadesEntidad.con().construir());
    }
}

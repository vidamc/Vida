/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.bloque.Bloque;
import dev.vida.bloque.MaterialBloque;
import dev.vida.bloque.PropiedadesBloque;
import dev.vida.core.Identifier;
import dev.vida.objeto.Herramienta;
import dev.vida.objeto.Material;
import dev.vida.objeto.Objeto;
import dev.vida.objeto.ObjetoDeBloque;
import dev.vida.objeto.PropiedadesObjeto;
import dev.vida.objeto.TipoObjeto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistroObjetosTest {

    private CatalogoManejador manejador;
    private RegistroObjetos reg;

    @BeforeEach
    void setUp() {
        manejador = new CatalogoManejador();
        reg = RegistroObjetos.conectar(manejador, "ejemplo");
    }

    private Objeto generico(String path) {
        return new Objeto(Identifier.of("ejemplo", path),
                PropiedadesObjeto.con().construir());
    }

    @Test
    void registrar_y_recuperar() {
        reg.registrarOExigir(generico("rubi"));
        assertThat(reg.cantidad()).isEqualTo(1);
        assertThat(reg.obtener("rubi")).isPresent();
    }

    @Test
    void registrar_duplicado_devuelve_error() {
        Objeto o = generico("dup");
        assertThat(reg.registrar(o).isOk()).isTrue();
        assertThat(reg.registrar(o).isErr()).isTrue();
    }

    @Test
    void etiquetas_funcionan() {
        Objeto pico = new Objeto(Identifier.of("ejemplo", "pico_hierro"),
                PropiedadesObjeto.con().herramienta(Herramienta.pico(Material.HIERRO)).construir());
        reg.registrarOExigir(pico,
                EtiquetaObjeto.de("minecraft", "pickaxes"),
                EtiquetaObjeto.de("minecraft", "tools"));
        assertThat(reg.miembros(EtiquetaObjeto.de("minecraft", "pickaxes")))
                .containsExactly(pico.id());
        assertThat(reg.contiene(EtiquetaObjeto.de("minecraft", "tools"), pico.id())).isTrue();
    }

    @Test
    void objeto_de_bloque_usa_id_del_bloque() {
        Bloque b = new Bloque(Identifier.of("ejemplo", "piedra_roja"),
                PropiedadesBloque.con(MaterialBloque.PIEDRA).construir());
        ObjetoDeBloque obj = ObjetoDeBloque.de(b);
        reg.registrarOExigir(obj);
        assertThat(obj.id()).isEqualTo(b.id());
        assertThat(obj.bloque()).isSameAs(b);
        assertThat(obj.tipo()).isEqualTo(TipoObjeto.BLOQUE);
    }

    @Test
    void objeto_de_bloque_id_override() {
        Bloque b = new Bloque(Identifier.of("ejemplo", "bloque"),
                PropiedadesBloque.con(MaterialBloque.PIEDRA).construir());
        ObjetoDeBloque obj = ObjetoDeBloque.conId(b,
                Identifier.of("ejemplo", "otro_id"),
                PropiedadesObjeto.con().tipo(TipoObjeto.BLOQUE).construir());
        assertThat(obj.id()).isEqualTo(Identifier.of("ejemplo", "otro_id"));
    }

    @Test
    void congelar_bloquea_registro() {
        reg.registrarOExigir(generico("a"));
        reg.congelar();
        assertThat(reg.registrar(generico("b")).isErr()).isTrue();
    }

    @Test
    void registrar_o_exigir_lanza_en_duplicado() {
        reg.registrarOExigir(generico("x"));
        assertThatThrownBy(() -> reg.registrarOExigir(generico("x")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constante_catalogo_id() {
        assertThat(RegistroObjetos.CATALOGO_ID).isEqualTo(Identifier.of("vida", "objeto"));
    }

    @Test
    void snapshot_etiquetas_inmutable() {
        Objeto o = generico("a");
        reg.registrarOExigir(o, EtiquetaObjeto.de("vida", "t"));
        var snap = reg.snapshotEtiquetas();
        assertThatThrownBy(() -> snap.put(Identifier.of("x", "y"), java.util.Set.of()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

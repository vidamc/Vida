/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.bloque.Bloque;
import dev.vida.bloque.MaterialBloque;
import dev.vida.bloque.PropiedadesBloque;
import dev.vida.bloque.TipoHerramienta;
import dev.vida.core.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistroBloquesTest {

    private CatalogoManejador manejador;
    private RegistroBloques reg;

    @BeforeEach
    void setUp() {
        manejador = new CatalogoManejador();
        reg = RegistroBloques.conectar(manejador, "ejemplo");
    }

    private Bloque bloque(String path, MaterialBloque m) {
        return new Bloque(
                Identifier.of("ejemplo", path),
                PropiedadesBloque.con(m).construir());
    }

    @Test
    void registrar_y_recuperar_por_id() {
        Bloque b = bloque("piedra_oscura", MaterialBloque.PIEDRA);
        assertThat(reg.registrar(b).isOk()).isTrue();

        assertThat(reg.obtener("piedra_oscura")).hasValueSatisfying(res ->
                assertThat(res.id()).isEqualTo(b.id()));
        assertThat(reg.cantidad()).isEqualTo(1);
    }

    @Test
    void registrar_duplicado_devuelve_error() {
        Bloque b = bloque("duplicado", MaterialBloque.PIEDRA);
        assertThat(reg.registrar(b).isOk()).isTrue();
        assertThat(reg.registrar(b).isErr()).isTrue();
    }

    @Test
    void etiquetar_y_comprobar_miembros() {
        Bloque piedra = bloque("piedra_oscura", MaterialBloque.PIEDRA);
        Bloque mineral = bloque("mineral_rojo", MaterialBloque.PIEDRA);
        reg.registrarOExigir(piedra,
                EtiquetaBloque.de("vida", "mineable/pico"));
        reg.registrarOExigir(mineral,
                EtiquetaBloque.de("vida", "mineable/pico"),
                EtiquetaBloque.de("ejemplo", "mineral"));

        EtiquetaBloque pico = EtiquetaBloque.de("vida", "mineable/pico");
        assertThat(reg.miembros(pico)).containsExactlyInAnyOrder(piedra.id(), mineral.id());
        assertThat(reg.contiene(pico, piedra.id())).isTrue();
        assertThat(reg.contiene(pico, Identifier.of("ejemplo", "nada"))).isFalse();
    }

    @Test
    void etiquetar_permite_id_no_registrado_como_futura_declaracion() {
        Identifier pendiente = Identifier.of("ejemplo", "pendiente");
        EtiquetaBloque tag = EtiquetaBloque.de("vida", "herbologia");
        reg.etiquetar(pendiente, tag);
        assertThat(reg.contiene(tag, pendiente)).isTrue();
    }

    @Test
    void snapshot_etiquetas_es_inmutable() {
        Bloque b = bloque("x", MaterialBloque.PIEDRA);
        reg.registrarOExigir(b, EtiquetaBloque.de("vida", "t"));
        var snap = reg.snapshotEtiquetas();
        assertThatThrownBy(() -> snap.put(Identifier.of("ejemplo", "z"), java.util.Set.of()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void compartido_con_otros_modulos_via_manejador() {
        reg.registrarOExigir(bloque("a", MaterialBloque.MADERA));

        RegistroBloques otro = RegistroBloques.conectar(manejador, "otro");
        assertThat(otro.cantidad()).isEqualTo(1);
        assertThat(otro.obtener(Identifier.of("ejemplo", "a"))).isPresent();
    }

    @Test
    void registrar_o_exigir_lanza_en_duplicado() {
        Bloque b = bloque("dup", MaterialBloque.PIEDRA);
        reg.registrarOExigir(b);
        assertThatThrownBy(() -> reg.registrarOExigir(b))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void congelar_bloquea_posteriores_registraciones() {
        reg.registrarOExigir(bloque("a", MaterialBloque.PIEDRA));
        reg.congelar();
        assertThat(reg.congelado()).isTrue();
        assertThat(reg.registrar(bloque("b", MaterialBloque.MADERA)).isErr()).isTrue();
    }

    @Test
    void constante_catalogo_id_es_vida_bloque() {
        assertThat(RegistroBloques.CATALOGO_ID).isEqualTo(Identifier.of("vida", "bloque"));
    }

    @Test
    void herramientas_requeridas_se_sobreviven_en_lectura() {
        Bloque diamante = new Bloque(
                Identifier.of("ejemplo", "diamante"),
                PropiedadesBloque.con(MaterialBloque.METAL)
                        .dureza(5f)
                        .nivelMinimo(dev.vida.bloque.NivelHerramienta.HIERRO)
                        .herramientas(TipoHerramienta.PICO)
                        .construir());
        reg.registrarOExigir(diamante);
        Bloque leido = reg.obtener(diamante.id()).orElseThrow();
        assertThat(leido.propiedades().herramientas()).containsExactly(TipoHerramienta.PICO);
        assertThat(leido.propiedades().requiereHerramienta()).isTrue();
    }
}

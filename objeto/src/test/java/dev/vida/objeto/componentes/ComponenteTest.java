/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.componentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.core.Identifier;
import dev.vida.objeto.Raridad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComponenteTest {

    @Test
    void durabilidad_dano_negativo_lanza() {
        assertThatThrownBy(() -> new Componente.Durabilidad(-1, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void durabilidad_dano_mayor_max_lanza() {
        assertThatThrownBy(() -> new Componente.Durabilidad(150, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void durabilidad_maximo_cero_lanza() {
        assertThatThrownBy(() -> new Componente.Durabilidad(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comida_valores_validos() {
        Componente.Comida c = new Componente.Comida(6, 0.6f, false, 32);
        assertThat(c.saciedad()).isEqualTo(6);
        assertThat(c.saturacion()).isEqualTo(0.6f);
    }

    @Test
    void comida_saciedad_fuera_de_rango_lanza() {
        assertThatThrownBy(() -> new Componente.Comida(21, 0f, false, 32))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void max_pila_fuera_de_rango_lanza() {
        assertThatThrownBy(() -> new Componente.MaxPila(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Componente.MaxPila(100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lore_inmutable() {
        Componente.Lore l = new Componente.Lore(List.of("línea 1", "línea 2"));
        assertThatThrownBy(() -> l.lineas().add("línea 3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void perfil_jugador_valido() {
        UUID u = UUID.randomUUID();
        Componente.PerfilJugador p = new Componente.PerfilJugador("Tester", Optional.of(u));
        assertThat(p.nombre()).isEqualTo("Tester");
        assertThat(p.uuid()).contains(u);
    }

    @Test
    void color_tinte_fuera_de_rango_lanza() {
        assertThatThrownBy(() -> new Componente.ColorTinte(0x10000000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void raro_con_raridad_valida() {
        Componente.Raro r = new Componente.Raro(Raridad.EPICO);
        assertThat(r.raridad()).isEqualTo(Raridad.EPICO);
    }

    @Test
    void mapa_obtener_por_clave_tipada() {
        MapaComponentes m = MapaComponentes.con()
                .poner(ClaveComponente.COMIDA, new Componente.Comida(4, 0.3f, false, 32))
                .poner(ClaveComponente.IRROMPIBLE, new Componente.Irrompible(true))
                .construir();

        assertThat(m.tamanio()).isEqualTo(2);
        assertThat(m.obtener(ClaveComponente.COMIDA))
                .hasValueSatisfying(c -> assertThat(c.saciedad()).isEqualTo(4));
        assertThat(m.contiene(ClaveComponente.IRROMPIBLE)).isTrue();
        assertThat(m.contiene(ClaveComponente.DURABILIDAD)).isFalse();
    }

    @Test
    void mapa_quitar_elimina_componente() {
        MapaComponentes m = MapaComponentes.con()
                .poner(ClaveComponente.IRROMPIBLE, new Componente.Irrompible(true))
                .quitar(ClaveComponente.IRROMPIBLE)
                .construir();
        assertThat(m.esVacio()).isTrue();
    }

    @Test
    void mapa_fusionar_sobrescribe() {
        MapaComponentes a = MapaComponentes.con()
                .poner(ClaveComponente.MAX_PILA, new Componente.MaxPila(16))
                .construir();
        MapaComponentes b = MapaComponentes.con()
                .poner(ClaveComponente.MAX_PILA, new Componente.MaxPila(32))
                .poner(ClaveComponente.IRROMPIBLE, new Componente.Irrompible(false))
                .construir();
        MapaComponentes out = a.fusionar(b);
        assertThat(out.obtener(ClaveComponente.MAX_PILA))
                .hasValueSatisfying(m -> assertThat(m.tamanio()).isEqualTo(32));
        assertThat(out.contiene(ClaveComponente.IRROMPIBLE)).isTrue();
    }

    @Test
    void clave_componente_rondabout_por_id_conocido() {
        assertThat(ClaveComponente.COMIDA.id()).isEqualTo(Identifier.parse("minecraft:food"));
        assertThat(ClaveComponente.DURABILIDAD.id()).isEqualTo(Identifier.parse("minecraft:damage"));
    }
}

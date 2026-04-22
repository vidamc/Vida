/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ResumenTest {

    @Test
    void fields_are_immutable_copies() {
        var metodos = new java.util.ArrayList<>(List.of(
                new Resumen.MetodoMuestra("com.example.Foo#bar", 100, 50.0)));
        var metricas = new java.util.ArrayList<>(List.of(
                new Resumen.LatidoMetrica("vida:arranque", 3, 10)));

        Resumen r = new Resumen(Duration.ofSeconds(5), 200, metodos, metricas, 2, 1, 100);

        metodos.clear();
        metricas.clear();

        assertThat(r.topMetodos()).hasSize(1);
        assertThat(r.latidoMetricas()).hasSize(1);
    }

    @Test
    void null_duracion_throws() {
        assertThatThrownBy(() ->
                new Resumen(null, 0, List.of(), List.of(), 0, 0, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void metodo_muestra_contract() {
        var m = new Resumen.MetodoMuestra("java.lang.Thread#sleep", 42, 21.0);
        assertThat(m.metodo()).isEqualTo("java.lang.Thread#sleep");
        assertThat(m.muestras()).isEqualTo(42);
        assertThat(m.porcentaje()).isEqualTo(21.0);
    }

    @Test
    void latido_metrica_contract() {
        var lm = new Resumen.LatidoMetrica("test:ev", 5, 100);
        assertThat(lm.nombre()).isEqualTo("test:ev");
        assertThat(lm.suscriptores()).isEqualTo(5);
        assertThat(lm.emisiones()).isEqualTo(100);
    }
}

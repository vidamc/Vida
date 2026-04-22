/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HiloPrincipalTest {

    @Test
    void pulso_ejecuta_en_orden_fifo() {
        HiloPrincipal hp = new HiloPrincipal();
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int v = i;
            hp.enviar(() -> out.add(v));
        }
        int hechas = hp.pulso();
        assertThat(hechas).isEqualTo(5);
        assertThat(out).containsExactly(0, 1, 2, 3, 4);
        assertThat(hp.pendientes()).isZero();
    }

    @Test
    void limite_por_pulso() throws InterruptedException {
        HiloPrincipal hp = new HiloPrincipal(3);
        for (int i = 0; i < 7; i++) {
            hp.enviar(() -> {});
        }
        assertThat(hp.pulso()).isEqualTo(3);
        assertThat(hp.pendientes()).isEqualTo(4);
        assertThat(hp.pulso()).isEqualTo(3);
        assertThat(hp.pulso()).isEqualTo(1);
        assertThat(hp.pendientes()).isZero();
    }

    @Test
    void callback_lanzando_no_detiene_cola() {
        HiloPrincipal hp = new HiloPrincipal();
        List<String> out = new ArrayList<>();
        hp.enviar(() -> { throw new RuntimeException("boom"); });
        hp.enviar(() -> out.add("tras-boom"));
        hp.pulso();
        assertThat(out).containsExactly("tras-boom");
    }

    @Test
    void pulso_desde_otro_hilo_lanza() throws Exception {
        HiloPrincipal hp = new HiloPrincipal();
        hp.pulso(); // первый раз — из теста, это main thread
        Thread t = new Thread(() ->
                assertThatThrownBy(hp::pulso).isInstanceOf(IllegalStateException.class));
        t.start();
        t.join();
    }

    @Test
    void reiniciar_despeja_la_cola() {
        HiloPrincipal hp = new HiloPrincipal();
        hp.enviar(() -> {});
        hp.enviar(() -> {});
        hp.reiniciar();
        assertThat(hp.pendientes()).isZero();
    }
}

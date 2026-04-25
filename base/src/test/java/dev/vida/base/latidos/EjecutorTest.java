/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.susurro.Etiqueta;
import dev.vida.susurro.HiloPrincipal;
import dev.vida.susurro.Susurro;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class EjecutorTest {

    record Ev(String n) {
        static final Latido<Ev> TIPO = Latido.de("test:ev", Ev.class);
    }

    @Test
    void sincrono_ejecuta_inline() {
        AtomicReference<Thread> threadObservado = new AtomicReference<>();
        Ejecutor.SINCRONO.ejecutar(() -> threadObservado.set(Thread.currentThread()));
        assertThat(threadObservado.get()).isSameAs(Thread.currentThread());
    }

    @Test
    void hiloPrincipal_marshalling_integrado_con_hp() {
        HiloPrincipal hp = new HiloPrincipal();
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicInteger contador = new AtomicInteger();
        bus.suscribir(Ev.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL,
                Ejecutor.hiloPrincipal(hp),
                e -> contador.incrementAndGet());

        LatidoDespacho d = bus.emitir(Ev.TIPO, new Ev("a"));
        // Задача попала в очередь главного потока → контадор ещё 0.
        assertThat(contador).hasValue(0);
        assertThat(d.recibidos()).isEqualTo(1);

        hp.pulso();
        assertThat(contador).hasValue(1);
    }

    @Test
    void susurro_ejecuta_asincrono() throws Exception {
        try (Susurro s = Susurro.iniciar()) {
            LatidoBus bus = LatidoBus.enMemoria();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> threadName = new AtomicReference<>();

            bus.suscribir(Ev.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL,
                    Ejecutor.susurro(s,
                            dev.vida.susurro.Prioridad.NORMAL, Etiqueta.de("test/ejecutor")),
                    e -> {
                        threadName.set(Thread.currentThread().getName());
                        latch.countDown();
                    });

            bus.emitir(Ev.TIPO, new Ev("x"));
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(threadName.get()).startsWith("vida-susurro-");
        }
    }

    @Test
    void serializado_mantiene_fifo_entre_subidas() throws Exception {
        Ejecutor ej = Ejecutor.serializado("t");
        List<Integer> orden = new ArrayList<>();
        CountDownLatch fin = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            ej.ejecutar(() -> {
                synchronized (orden) { orden.add(idx); }
                fin.countDown();
            });
        }
        assertThat(fin.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(orden).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    void fallos_en_ejecutor_no_matan_otros_oyentes() throws Exception {
        try (Susurro s = Susurro.iniciar()) {
            LatidoBus bus = LatidoBus.enMemoria();
            AtomicInteger ok = new AtomicInteger();
            CountDownLatch fin = new CountDownLatch(2);

            bus.suscribir(Ev.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL,
                    Ejecutor.susurro(s), e -> {
                        fin.countDown();
                        throw new RuntimeException("boom");
                    });
            bus.suscribir(Ev.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL,
                    Ejecutor.susurro(s), e -> { ok.incrementAndGet(); fin.countDown(); });

            bus.emitir(Ev.TIPO, new Ev("y"));
            assertThat(fin.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(ok).hasValue(1);
        }
    }
}

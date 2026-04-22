/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SusurroTest {

    @Test
    void tarea_simple_devuelve_resultado() throws Exception {
        try (Susurro sus = Susurro.iniciar()) {
            Tarea<Integer> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("test/simple"),
                    () -> 42);
            assertThat(t.esperar(Duration.ofSeconds(2))).isEqualTo(42);
            assertThat(t.estado()).isEqualTo(Tarea.Estado.COMPLETADA);
        }
    }

    @Test
    void tarea_que_lanza_termina_fallada() throws Exception {
        try (Susurro sus = Susurro.iniciar()) {
            Tarea<Integer> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("test/boom"),
                    () -> { throw new IllegalStateException("ups"); });
            assertThatThrownBy(() -> t.esperar(Duration.ofSeconds(2)))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
            assertThat(t.estado()).isEqualTo(Tarea.Estado.FALLADA);
        }
    }

    @Test
    void tarea_cancelada_antes_de_iniciar() throws Exception {
        Susurro sus = Susurro.iniciar(new Susurro.Politica(1, 16, 0));
        try {
            CountDownLatch cerrojo = new CountDownLatch(1);
            // Первая блокирующая задача, занимает единственного worker'а
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("x"), () -> {
                try { cerrojo.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return 1;
            });
            // Вторую отменяем до начала
            Tarea<Integer> t2 = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("x"), () -> 2);
            t2.cancelar();
            assertThat(t2.revisada()).isTrue();
            cerrojo.countDown();
        } finally {
            sus.detener();
        }
    }

    @Test
    void prioridad_alta_vence_a_baja() throws Exception {
        Susurro sus = Susurro.iniciar(new Susurro.Politica(1, 1024, 0));
        try {
            CountDownLatch barrera = new CountDownLatch(1);
            // Блокирующая — занимает worker'а
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("block"),
                    () -> { try { barrera.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } return 0; });

            // Ставим в очередь: сначала BAJA, потом ALTA.
            List<String> orden = new java.util.concurrent.CopyOnWriteArrayList<>();
            Tarea<Integer> baja = sus.lanzar(Prioridad.BAJA, Etiqueta.de("q"),
                    () -> { orden.add("baja"); return 1; });
            Tarea<Integer> alta = sus.lanzar(Prioridad.ALTA, Etiqueta.de("q"),
                    () -> { orden.add("alta"); return 2; });

            barrera.countDown();
            baja.esperar(Duration.ofSeconds(2));
            alta.esperar(Duration.ofSeconds(2));

            assertThat(orden).containsExactly("alta", "baja");
        } finally {
            sus.detener();
        }
    }

    @Test
    void back_pressure_por_max_cola() throws Exception {
        Susurro sus = Susurro.iniciar(new Susurro.Politica(1, 1, 0));
        try {
            CountDownLatch sostener = new CountDownLatch(1);
            // Занимаем worker'а
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("a"),
                    () -> { try { sostener.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } return 0; });
            // Первая в очереди
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("a"), () -> 1);
            // Вторая должна быть отклонена (maxCola = 1 — уже заняты)
            Tarea<Integer> rechazada = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("a"), () -> 2);
            assertThatThrownBy(() -> rechazada.esperar(Duration.ofSeconds(1)))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RejectedExecutionException.class);
            sostener.countDown();
        } finally {
            sus.detener();
        }
    }

    @Test
    void lim_por_etiqueta_rechaza() throws Exception {
        Susurro sus = Susurro.iniciar(new Susurro.Politica(2, 16, 1));
        try {
            CountDownLatch c = new CountDownLatch(1);
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("solo"),
                    () -> { try { c.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } return 0; });
            Tarea<Integer> dup = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("solo"), () -> 1);
            assertThatThrownBy(() -> dup.esperar(Duration.ofSeconds(1)))
                    .hasCauseInstanceOf(RejectedExecutionException.class);
            c.countDown();
        } finally {
            sus.detener();
        }
    }

    @Test
    void lim_por_etiqueta_no_afecta_otras() throws Exception {
        Susurro sus = Susurro.iniciar(new Susurro.Politica(2, 16, 1));
        try {
            CountDownLatch c = new CountDownLatch(1);
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("solo"),
                    () -> { try { c.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } return 0; });
            Tarea<Integer> otra = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("otra"), () -> 7);
            assertThat(otra.esperar(Duration.ofSeconds(2))).isEqualTo(7);
            c.countDown();
        } finally {
            sus.detener();
        }
    }

    @Test
    void plazo_aborta_larga_tarea() {
        try (Susurro sus = Susurro.iniciar()) {
            Tarea<Integer> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("lenta"), () -> {
                try { Thread.sleep(5_000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return 1;
            }).conPlazo(Duration.ofMillis(150));
            assertThatThrownBy(() -> t.esperar(Duration.ofSeconds(1)))
                    .isInstanceOf(ExecutionException.class);
            assertThat(t.estado()).isEqualTo(Tarea.Estado.CANCELADA);
        }
    }

    @Test
    void integracion_hilo_principal() throws Exception {
        HiloPrincipal hp = new HiloPrincipal();
        try (Susurro sus = Susurro.iniciar()) {
            AtomicInteger visto = new AtomicInteger();
            Tarea<Integer> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("hp"), () -> 99)
                    .enHiloPrincipal(hp, visto::set);
            t.esperar(Duration.ofSeconds(2));
            // Результат пока не доставлен — нужно «тикнуть»
            // (allowing for async whenComplete to enqueue)
            for (int i = 0; i < 50 && hp.pendientes() == 0; i++) {
                Thread.sleep(5);
            }
            hp.pulso();
            assertThat(visto.get()).isEqualTo(99);
        }
    }

    @Test
    void estadisticas_monitoran_trabajo() throws Exception {
        try (Susurro sus = Susurro.iniciar(new Susurro.Politica(2, 16, 0))) {
            CountDownLatch l = new CountDownLatch(1);
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("x"),
                    () -> { try { l.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } return 0; });
            Thread.sleep(100);
            Susurro.Estadisticas est = sus.estadisticas();
            assertThat(est.workers()).isEqualTo(2);
            assertThat(est.pendientes()).isGreaterThanOrEqualTo(1);
            l.countDown();
            Thread.sleep(100);
        }
    }

    @Test
    void mapa_transforma_resultado() throws Exception {
        try (Susurro sus = Susurro.iniciar()) {
            Tarea<String> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("m"), () -> 21)
                    .mapa(v -> Integer.toString(v * 2));
            assertThat(t.esperar(Duration.ofSeconds(1))).isEqualTo("42");
        }
    }

    @Test
    void determinismo_fifo_dentro_de_mismo_prioridad() throws Exception {
        Susurro sus = Susurro.iniciar(new Susurro.Politica(1, 1024, 0));
        try {
            CountDownLatch puerta = new CountDownLatch(1);
            sus.lanzar(Prioridad.NORMAL, Etiqueta.de("puerta"),
                    () -> { try { puerta.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } return 0; });

            List<Integer> orden = new java.util.concurrent.CopyOnWriteArrayList<>();
            List<Tarea<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                int v = i;
                tareas.add(sus.lanzar(Prioridad.NORMAL, Etiqueta.de("fifo"), () -> {
                    orden.add(v);
                    return v;
                }));
            }
            puerta.countDown();
            for (Tarea<Integer> t : tareas) t.esperar(Duration.ofSeconds(2));
            assertThat(orden).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        } finally {
            sus.detener();
        }
    }

    @Test
    void politica_invalida_lanza() {
        assertThatThrownBy(() -> new Susurro.Politica(0, 16, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Susurro.Politica(1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Susurro.Politica(1, 16, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detener_es_idempotente() throws InterruptedException {
        Susurro sus = Susurro.iniciar();
        sus.detener();
        sus.detener();
        TimeUnit.MILLISECONDS.sleep(50);
    }
}

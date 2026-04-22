/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class LatidoBusTest {

    record EventoSaludo(String nombre) {
        static final Latido<EventoSaludo> TIPO = Latido.de("test:saludo", EventoSaludo.class);
    }

    static final class EventoCancelable implements LatidoCancelable {
        private volatile boolean cancelado;
        @Override public boolean cancelado() { return cancelado; }
        @Override public void cancelar() { cancelado = true; }

        static final Latido<EventoCancelable> TIPO =
                Latido.de("test:cancelable", EventoCancelable.class);
    }

    // ----------------------------------------------------------------

    @Test
    void latido_cancelable_properly_detected() {
        assertThat(EventoSaludo.TIPO.esCancelable()).isFalse();
        assertThat(EventoCancelable.TIPO.esCancelable()).isTrue();
    }

    @Test
    void basic_publish_subscribe() {
        LatidoBus bus = LatidoBus.enMemoria();
        List<String> recibido = new ArrayList<>();
        bus.suscribir(EventoSaludo.TIPO, e -> recibido.add(e.nombre()));

        LatidoDespacho d = bus.emitir(EventoSaludo.TIPO, new EventoSaludo("mundo"));
        assertThat(d.recibidos()).isEqualTo(1);
        assertThat(d.tieneErrores()).isFalse();
        assertThat(recibido).containsExactly("mundo");
    }

    @Test
    void priority_ordering_is_deterministic() {
        LatidoBus bus = LatidoBus.enMemoria();
        List<String> orden = new ArrayList<>();

        bus.suscribir(EventoSaludo.TIPO, Prioridad.BAJA, e -> orden.add("BAJA"));
        bus.suscribir(EventoSaludo.TIPO, Prioridad.URGENTE, e -> orden.add("URGENTE"));
        bus.suscribir(EventoSaludo.TIPO, Prioridad.NORMAL, e -> orden.add("NORMAL"));
        bus.suscribir(EventoSaludo.TIPO, Prioridad.ALTA, e -> orden.add("ALTA"));
        bus.suscribir(EventoSaludo.TIPO, Prioridad.MONITOR, e -> orden.add("MONITOR"));

        bus.emitir(EventoSaludo.TIPO, new EventoSaludo("x"));
        assertThat(orden).containsExactly("URGENTE", "ALTA", "NORMAL", "BAJA", "MONITOR");
    }

    @Test
    void fase_ordering_within_priority() {
        LatidoBus bus = LatidoBus.enMemoria();
        List<String> orden = new ArrayList<>();

        bus.suscribir(EventoSaludo.TIPO, Prioridad.NORMAL, Fase.DESPUES, e -> orden.add("DESPUES"));
        bus.suscribir(EventoSaludo.TIPO, Prioridad.NORMAL, Fase.ANTES,   e -> orden.add("ANTES"));
        bus.suscribir(EventoSaludo.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL, e -> orden.add("PRINCIPAL"));

        bus.emitir(EventoSaludo.TIPO, new EventoSaludo("x"));
        assertThat(orden).containsExactly("ANTES", "PRINCIPAL", "DESPUES");
    }

    @Test
    void fifo_for_equal_priority_and_fase() {
        LatidoBus bus = LatidoBus.enMemoria();
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            bus.suscribir(EventoSaludo.TIPO, e -> orden.add(idx));
        }
        bus.emitir(EventoSaludo.TIPO, new EventoSaludo("x"));
        assertThat(orden).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    void cancellation_stops_normal_listeners_but_not_monitor() {
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicBoolean monitorInvoked = new AtomicBoolean();
        AtomicBoolean normalAfterInvoked = new AtomicBoolean();

        bus.suscribir(EventoCancelable.TIPO, Prioridad.URGENTE, EventoCancelable::cancelar);
        bus.suscribir(EventoCancelable.TIPO, Prioridad.NORMAL, e -> normalAfterInvoked.set(true));
        bus.suscribir(EventoCancelable.TIPO, Prioridad.MONITOR, e -> monitorInvoked.set(true));

        LatidoDespacho d = bus.emitir(EventoCancelable.TIPO, new EventoCancelable());
        assertThat(d.cancelado()).isTrue();
        assertThat(monitorInvoked).isTrue();
        assertThat(normalAfterInvoked).isFalse();
    }

    @Test
    void listener_throwing_is_trapped() {
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicInteger other = new AtomicInteger();

        bus.suscribir(EventoSaludo.TIPO, Prioridad.URGENTE, e -> {
            throw new RuntimeException("boom");
        });
        bus.suscribir(EventoSaludo.TIPO, Prioridad.NORMAL, e -> other.incrementAndGet());

        LatidoDespacho d = bus.emitir(EventoSaludo.TIPO, new EventoSaludo("x"));
        assertThat(d.tieneErrores()).isTrue();
        assertThat(d.errores()).hasSize(1);
        assertThat(d.recibidos()).isEqualTo(1);
        assertThat(other).hasValue(1);
    }

    @Test
    void unsubscribe_removes_listener() {
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicInteger n = new AtomicInteger();
        Suscripcion s = bus.suscribir(EventoSaludo.TIPO, e -> n.incrementAndGet());

        bus.emitir(EventoSaludo.TIPO, new EventoSaludo("1"));
        assertThat(n).hasValue(1);
        assertThat(s.activa()).isTrue();

        s.cancelar();
        assertThat(s.activa()).isFalse();

        bus.emitir(EventoSaludo.TIPO, new EventoSaludo("2"));
        assertThat(n).hasValue(1);
    }

    @Test
    void try_with_resources_auto_cancels() {
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicInteger n = new AtomicInteger();
        try (Suscripcion s = bus.suscribir(EventoSaludo.TIPO, e -> n.incrementAndGet())) {
            bus.emitir(EventoSaludo.TIPO, new EventoSaludo("1"));
            assertThat(s.activa()).isTrue();
        }
        bus.emitir(EventoSaludo.TIPO, new EventoSaludo("2"));
        assertThat(n).hasValue(1);
    }

    @Test
    void emit_without_listeners_is_harmless() {
        LatidoBus bus = LatidoBus.enMemoria();
        LatidoDespacho d = bus.emitir(EventoSaludo.TIPO, new EventoSaludo("x"));
        assertThat(d.recibidos()).isZero();
        assertThat(d.errores()).isEmpty();
    }

    @Test
    void concurrent_subscribe_and_emit() throws Exception {
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicInteger totalRecibidos = new AtomicInteger();

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        // Половина подписывается, половина публикует.
        for (int i = 0; i < threads / 2; i++) {
            pool.submit(() -> {
                start.await();
                for (int j = 0; j < 100; j++) {
                    bus.suscribir(EventoSaludo.TIPO, e -> totalRecibidos.incrementAndGet());
                }
                return null;
            });
        }
        for (int i = 0; i < threads / 2; i++) {
            pool.submit(() -> {
                start.await();
                for (int j = 0; j < 50; j++) {
                    bus.emitir(EventoSaludo.TIPO, new EventoSaludo("x"));
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        // Мы не знаем точное число (зависит от порядка), но как минимум
        // шина не уронила JVM и подписки/диспетч прошли.
        assertThat(bus.cantidadSuscriptores(EventoSaludo.TIPO))
                .isEqualTo(400); // 4 потока × 100 подписок
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void type_mismatch_on_emit_throws() {
        LatidoBus bus = LatidoBus.enMemoria();
        final Latido tipoRaw = EventoSaludo.TIPO;
        assertThatThrownBy(() -> bus.emitir(tipoRaw, "plain string"))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void limpiar_removes_everything() {
        LatidoBus bus = LatidoBus.enMemoria();
        bus.suscribir(EventoSaludo.TIPO, e -> {});
        bus.suscribir(EventoSaludo.TIPO, e -> {});
        assertThat(bus.cantidadSuscriptores(EventoSaludo.TIPO)).isEqualTo(2);
        bus.limpiar();
        assertThat(bus.cantidadSuscriptores(EventoSaludo.TIPO)).isZero();
    }
}

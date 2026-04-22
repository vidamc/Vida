/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.susurro.HiloPrincipal;
import dev.vida.susurro.Susurro;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class LatidoRegistradorTest {

    // ---------------------------------------------------------------
    //  Event types
    // ---------------------------------------------------------------

    record EvSimple(String msg) {
        static final Latido<EvSimple> TIPO = Latido.de("test:simple", EvSimple.class);
    }

    record EvOtro(int n) {
        static final Latido<EvOtro> TIPO = Latido.de("test:otro", EvOtro.class);
    }

    record EvSinLatido(String x) {}

    // ---------------------------------------------------------------
    //  Subscriber classes
    // ---------------------------------------------------------------

    static class OyenteSincrono {
        final AtomicInteger llamadas = new AtomicInteger();

        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void alSimple(EvSimple ev) {
            llamadas.incrementAndGet();
        }
    }

    static class OyenteDoble {
        final AtomicInteger simples = new AtomicInteger();
        final AtomicInteger otros = new AtomicInteger();

        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void alSimple(EvSimple ev) {
            simples.incrementAndGet();
        }

        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void alOtro(EvOtro ev) {
            otros.incrementAndGet();
        }
    }

    static class OyenteHiloPrincipal {
        final AtomicReference<String> threadName = new AtomicReference<>();

        @EjecutorLatido(kind = EjecutorLatido.Kind.HILO_PRINCIPAL)
        public void alSimple(EvSimple ev) {
            threadName.set(Thread.currentThread().getName());
        }
    }

    static class OyenteSusurro {
        final AtomicReference<String> threadName = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        @EjecutorLatido(kind = EjecutorLatido.Kind.SUSURRO, etiqueta = "test/reg")
        public void alSimple(EvSimple ev) {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }
    }

    static class OyentePrioridadCustom {
        final AtomicInteger llamadas = new AtomicInteger();

        @EjecutorLatido(
                kind = EjecutorLatido.Kind.SINCRONO,
                prioridadBus = EjecutorLatido.PrioridadBus.ALTA,
                fase = EjecutorLatido.FaseBus.ANTES)
        public void alSimple(EvSimple ev) {
            llamadas.incrementAndGet();
        }
    }

    static class OyenteSinParametro {
        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void malo() {}
    }

    static class OyenteDosParametros {
        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void malo(EvSimple a, EvOtro b) {}
    }

    static class OyenteSinLatidoField {
        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void alSinLatido(EvSinLatido ev) {}
    }

    static class OyenteSusurroSinPool {
        @EjecutorLatido(kind = EjecutorLatido.Kind.SUSURRO)
        public void alSimple(EvSimple ev) {}
    }

    static class OyenteHpSinHp {
        @EjecutorLatido(kind = EjecutorLatido.Kind.HILO_PRINCIPAL)
        public void alSimple(EvSimple ev) {}
    }

    // ---------------------------------------------------------------
    //  Subscriber with Latido field on the owner class
    // ---------------------------------------------------------------

    static class OyenteConLatidoLocal {
        static final Latido<EvSinLatido> EV_LOCAL =
                Latido.de("test:local", EvSinLatido.class);

        final AtomicInteger llamadas = new AtomicInteger();

        @EjecutorLatido(kind = EjecutorLatido.Kind.SINCRONO)
        public void alLocal(EvSinLatido ev) {
            llamadas.incrementAndGet();
        }
    }

    static class OyenteTickCadaCuatro {
        final AtomicInteger llamadas = new AtomicInteger();
        final AtomicReference<Long> ultimoTick = new AtomicReference<>();

        @OyenteDeTick(tps = 5)
        public void alTick(LatidoPulso ev) {
            llamadas.incrementAndGet();
            ultimoTick.set(ev.tickActual());
        }
    }

    static class OyenteTickSusurro {
        final AtomicReference<String> threadName = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        @OyenteDeTick(tps = 20, kind = EjecutorLatido.Kind.SUSURRO, etiqueta = "test/tick")
        public void alTick(LatidoPulso ev) {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }
    }

    static class OyenteTickTpsInvalido {
        @OyenteDeTick(tps = 0)
        public void alTick(LatidoPulso ev) {}
    }

    static class OyenteTickConflictivo {
        @OyenteDeTick
        @EjecutorLatido
        public void alTick(LatidoPulso ev) {}
    }

    // ---------------------------------------------------------------
    //  Tests
    // ---------------------------------------------------------------

    @Test
    void sincrono_registra_y_entrega() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteSincrono();

        List<Suscripcion> subs = LatidoRegistrador.registrarEnObjeto(bus, obj);

        assertThat(subs).hasSize(1);
        assertThat(subs.getFirst().activa()).isTrue();

        bus.emitir(EvSimple.TIPO, new EvSimple("hola"));
        assertThat(obj.llamadas).hasValue(1);

        bus.emitir(EvSimple.TIPO, new EvSimple("mundo"));
        assertThat(obj.llamadas).hasValue(2);
    }

    @Test
    void multiples_metodos_registran_correctamente() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteDoble();

        List<Suscripcion> subs = LatidoRegistrador.registrarEnObjeto(bus, obj);
        assertThat(subs).hasSize(2);

        bus.emitir(EvSimple.TIPO, new EvSimple("x"));
        bus.emitir(EvOtro.TIPO, new EvOtro(42));

        assertThat(obj.simples).hasValue(1);
        assertThat(obj.otros).hasValue(1);
    }

    @Test
    void cancelar_suscripcion_desregistra() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteSincrono();

        List<Suscripcion> subs = LatidoRegistrador.registrarEnObjeto(bus, obj);
        subs.forEach(Suscripcion::cancelar);

        bus.emitir(EvSimple.TIPO, new EvSimple("ignorado"));
        assertThat(obj.llamadas).hasValue(0);
    }

    @Test
    void hiloPrincipal_ejecuta_en_pulso() {
        LatidoBus bus = LatidoBus.enMemoria();
        HiloPrincipal hp = new HiloPrincipal();
        var obj = new OyenteHiloPrincipal();

        LatidoRegistrador.registrarEnObjeto(bus, obj, null, hp);
        bus.emitir(EvSimple.TIPO, new EvSimple("hp"));

        assertThat(obj.threadName.get()).isNull();
        hp.pulso();
        assertThat(obj.threadName.get()).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    void susurro_ejecuta_asincrono() throws Exception {
        try (Susurro s = Susurro.iniciar()) {
            LatidoBus bus = LatidoBus.enMemoria();
            var obj = new OyenteSusurro();

            LatidoRegistrador.registrarEnObjeto(bus, obj, s, null);
            bus.emitir(EvSimple.TIPO, new EvSimple("async"));

            assertThat(obj.latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(obj.threadName.get()).startsWith("vida-susurro-");
        }
    }

    @Test
    void prioridad_y_fase_custom() {
        LatidoBus bus = LatidoBus.enMemoria();
        var custom = new OyentePrioridadCustom();
        var normal = new OyenteSincrono();

        LatidoRegistrador.registrarEnObjeto(bus, custom);
        LatidoRegistrador.registrarEnObjeto(bus, normal);

        assertThat(bus.cantidadSuscriptores(EvSimple.TIPO)).isEqualTo(2);

        bus.emitir(EvSimple.TIPO, new EvSimple("test"));
        assertThat(custom.llamadas).hasValue(1);
        assertThat(normal.llamadas).hasValue(1);
    }

    @Test
    void latido_en_clase_owner_funciona() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteConLatidoLocal();

        List<Suscripcion> subs = LatidoRegistrador.registrarEnObjeto(bus, obj);
        assertThat(subs).hasSize(1);

        bus.emitir(OyenteConLatidoLocal.EV_LOCAL, new EvSinLatido("local"));
        assertThat(obj.llamadas).hasValue(1);
    }

    @Test
    void sin_parametros_lanza_FirmaInvalida() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteSinParametro();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, obj))
                .isInstanceOf(LatidoRegistradorError.FirmaInvalida.class)
                .hasMessageContaining("ровно 1 параметр")
                .hasMessageContaining("0");
    }

    @Test
    void dos_parametros_lanza_FirmaInvalida() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteDosParametros();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, obj))
                .isInstanceOf(LatidoRegistradorError.FirmaInvalida.class)
                .hasMessageContaining("2");
    }

    @Test
    void sin_latido_lanza_LatidoNoEncontrado() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteSinLatidoField();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, obj))
                .isInstanceOf(LatidoRegistradorError.LatidoNoEncontrado.class)
                .hasMessageContaining("EvSinLatido");
    }

    @Test
    void susurro_sin_pool_lanza_EjecutorFaltante() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteSusurroSinPool();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, obj))
                .isInstanceOf(LatidoRegistradorError.EjecutorFaltante.class)
                .hasMessageContaining("SUSURRO");
    }

    @Test
    void hiloPrincipal_sin_hp_lanza_EjecutorFaltante() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteHpSinHp();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, obj))
                .isInstanceOf(LatidoRegistradorError.EjecutorFaltante.class)
                .hasMessageContaining("HILO_PRINCIPAL");
    }

    @Test
    void objeto_sin_anotaciones_retorna_lista_vacia() {
        LatidoBus bus = LatidoBus.enMemoria();
        List<Suscripcion> subs = LatidoRegistrador.registrarEnObjeto(bus, "un string");
        assertThat(subs).isEmpty();
    }

    @Test
    void oyenteDeTick_filtra_por_tps_y_omite_subticks() {
        LatidoBus bus = LatidoBus.enMemoria();
        var obj = new OyenteTickCadaCuatro();

        LatidoRegistrador.registrarEnObjeto(bus, obj);
        for (long tick = 0; tick < 20; tick++) {
            bus.emitir(LatidoPulso.TIPO, new LatidoPulso(tick, "overworld", 0));
            bus.emitir(LatidoPulso.TIPO, new LatidoPulso(tick, "overworld", 1));
        }

        assertThat(obj.llamadas).hasValue(5);
        assertThat(obj.ultimoTick.get()).isEqualTo(16L);
    }

    @Test
    void oyenteDeTick_susurro_ejecuta_asincrono() throws Exception {
        try (Susurro s = Susurro.iniciar()) {
            LatidoBus bus = LatidoBus.enMemoria();
            var obj = new OyenteTickSusurro();

            LatidoRegistrador.registrarEnObjeto(bus, obj, s, null);
            bus.emitir(LatidoPulso.TIPO, LatidoPulso.raiz(0));

            assertThat(obj.latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(obj.threadName.get()).startsWith("vida-susurro-");
        }
    }

    @Test
    void oyenteDeTick_tps_invalido_lanza_error() {
        LatidoBus bus = LatidoBus.enMemoria();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, new OyenteTickTpsInvalido()))
                .isInstanceOf(LatidoRegistradorError.TpsInvalido.class)
                .hasMessageContaining("tps");
    }

    @Test
    void oyenteDeTick_y_ejecutorLatido_juntos_lanzan_conflicto() {
        LatidoBus bus = LatidoBus.enMemoria();

        assertThatThrownBy(() -> LatidoRegistrador.registrarEnObjeto(bus, new OyenteTickConflictivo()))
                .isInstanceOf(LatidoRegistradorError.AnotacionesConflictivas.class)
                .hasMessageContaining("@EjecutorLatido")
                .hasMessageContaining("@OyenteDeTick");
    }

    @Test
    void integracion_susurro_hiloPrincipal_juntos() throws Exception {
        try (Susurro s = Susurro.iniciar()) {
            HiloPrincipal hp = new HiloPrincipal();
            LatidoBus bus = LatidoBus.enMemoria();

            var oyenteSus = new OyenteSusurro();
            var oyenteHp = new OyenteHiloPrincipal();

            LatidoRegistrador.registrarEnObjeto(bus, oyenteSus, s, hp);
            LatidoRegistrador.registrarEnObjeto(bus, oyenteHp, s, hp);

            bus.emitir(EvSimple.TIPO, new EvSimple("dual"));

            assertThat(oyenteSus.latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(oyenteSus.threadName.get()).startsWith("vida-susurro-");

            hp.pulso();
            assertThat(oyenteHp.threadName.get()).isEqualTo(Thread.currentThread().getName());
        }
    }
}

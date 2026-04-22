/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Identifier;
import dev.vida.core.Result;
import java.util.List;
import org.junit.jupiter.api.Test;

class TejidoCanalTest {

    @Test
    void serializa_y_deserializa_record_automaticamente() {
        TejidoCanal canal = new TejidoCanal(8);
        canal.registrarRecordCliente(PingCliente.class, 1);

        PingCliente enviado = new PingCliente(42, Identifier.of("demo", "p1"), true, EstadoPing.OK);
        Result<TramaPaquete, TejidoError> encolado = canal.encolar(enviado, 1);
        assertThat(encolado.isOk()).isTrue();

        Result<Record, TejidoError> recibido = canal.decodificar(encolado.unwrap());
        assertThat(recibido.isOk()).isTrue();
        assertThat(recibido.unwrap()).isEqualTo(enviado);
    }

    @Test
    void decode_usa_version_menor_compatible_si_no_hay_exacta() {
        TejidoCanal canal = new TejidoCanal(4);
        canal.registrarCliente(PingCliente.class, 1, new CodecPaquete<>() {
            @Override
            public byte[] codificar(PingCliente paquete) {
                return new byte[] {(byte) paquete.seq()};
            }

            @Override
            public PingCliente decodificar(byte[] payload) {
                return new PingCliente(payload[0], Identifier.of("vida", "compat"), false, EstadoPing.OK);
            }
        });

        TramaPaquete trama = new TramaPaquete(
                DireccionPaquete.CLIENTE_A_SERVIDOR,
                PingCliente.class.getCanonicalName(),
                3,
                new byte[] {9});
        Result<Record, TejidoError> decoded = canal.decodificar(trama);

        assertThat(decoded.isOk()).isTrue();
        assertThat(decoded.unwrap()).isEqualTo(new PingCliente(9, Identifier.of("vida", "compat"), false, EstadoPing.OK));
    }

    @Test
    void aplica_backpressure_cuando_la_cola_esta_llena() {
        TejidoCanal canal = new TejidoCanal(1);
        canal.registrarRecordServidor(PongServidor.class, 1);

        assertThat(canal.encolar(new PongServidor("a"), 1).isOk()).isTrue();
        Result<TramaPaquete, TejidoError> segundo = canal.encolar(new PongServidor("b"), 1);

        assertThat(segundo.isErr()).isTrue();
        assertThat(segundo.unwrapErr()).isInstanceOf(TejidoError.BackPressure.class);
        assertThat(canal.estadisticas().rechazadosBackPressure()).isEqualTo(1L);
    }

    @Test
    void drenar_vacia_la_cola() {
        TejidoCanal canal = new TejidoCanal(4);
        canal.registrarRecordServidor(PongServidor.class, 1);
        canal.encolar(new PongServidor("one"), 1);
        canal.encolar(new PongServidor("two"), 1);

        List<TramaPaquete> frames = canal.drenarPendientes();
        assertThat(frames).hasSize(2);
        assertThat(canal.pendientes()).isZero();
    }

    @Test
    void error_si_tipo_no_registrado() {
        TejidoCanal canal = new TejidoCanal(2);

        Result<TramaPaquete, TejidoError> r = canal.encolar(new PongServidor("hola"), 1);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(TejidoError.TipoNoRegistrado.class);
    }

    record PingCliente(int seq, Identifier id, boolean urgente, EstadoPing estado) implements PaqueteCliente {}

    record PongServidor(String mensaje) implements PaqueteServidor {}

    enum EstadoPing {
        OK,
        FAIL
    }
}

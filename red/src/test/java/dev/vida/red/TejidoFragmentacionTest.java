/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Result;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class TejidoFragmentacionTest {

    public record EchoBytes(byte[] data) implements PaqueteCliente {}

    @Test
    void partition_roundtrip_cliente() {
        TejidoCanal canal = new TejidoCanal(512);
        TejidoFragmentacion.registrarFragmentos(canal);
        canal.registrarCliente(EchoBytes.class, 1, CodificadorRegistros.para(EchoBytes.class));

        byte[] big = new byte[1_200_000];
        Arrays.fill(big, (byte) 7);
        CodecPaquete<EchoBytes> codec = CodificadorRegistros.para(EchoBytes.class);
        Result<List<TramaPaquete>, TejidoError> r =
                TejidoFragmentacion.encolarParticionadoCliente(
                        canal, codec, new EchoBytes(big), 1, 20_000);
        assertThat(r.isOk()).isTrue();
        List<TramaPaquete> tramas = r.unwrap();
        assertThat(tramas.size()).isGreaterThan(1);

        TejidoEnsambladorFragmentos ens = new TejidoEnsambladorFragmentos(2_000_000);
        byte[] rebuilt = null;
        for (TramaPaquete t : tramas) {
            Result<Record, TejidoError> dec = canal.decodificar(t);
            assertThat(dec.isOk()).isTrue();
            PaqueteClienteCargaFragmento frag = (PaqueteClienteCargaFragmento) dec.unwrap();
            var acc = ens.aceptarCliente(frag);
            assertThat(acc.isOk()).isTrue();
            if (acc.unwrap().isPresent()) {
                rebuilt = acc.unwrap().get();
            }
        }
        assertThat(rebuilt).isNotNull();
        EchoBytes round = codec.decodificar(rebuilt);
        assertThat(round.data()).isEqualTo(big);
    }

    @Test
    void ajustes_sinc_packet_codec() {
        var p = new PaqueteAjustesSincronizacionServidor("demo", Map.of("a.b", "1"));
        var codec = PaqueteAjustesSincronizacionServidor.codec();
        byte[] w = codec.codificar(p);
        PaqueteAjustesSincronizacionServidor q = codec.decodificar(w);
        assertThat(q).isEqualTo(p);
    }
}

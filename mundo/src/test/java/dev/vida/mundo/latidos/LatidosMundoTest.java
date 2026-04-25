/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo.latidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.Identifier;
import dev.vida.mundo.Bioma;
import dev.vida.mundo.Coordenada;
import dev.vida.mundo.Dimension;
import dev.vida.mundo.Mundo;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LatidosMundoTest {

    private static final Mundo MUNDO = new MundoFalso();

    @Test
    void eventos_tienen_tipo_estable_y_entregan_payload() {
        LatidoBus bus = LatidoBus.enMemoria();
        AtomicReference<String> recibido = new AtomicReference<>();

        bus.suscribir(LatidosMundo.MundoCargado.TIPO, ev -> recibido.set(ev.mundo().id().toString()));
        bus.emitir(LatidosMundo.MundoCargado.TIPO, new LatidosMundo.MundoCargado(MUNDO, true));

        assertThat(recibido.get()).isEqualTo("vida:test");
        assertThat(LatidosMundo.Tick.TIPO.id().toString()).isEqualTo("vida:mundo_tick");
        assertThat(LatidosMundo.ChunkDescargado.TIPO.id().toString()).isEqualTo("vida:chunk_descargado");

        AtomicReference<int[]> chunkDesc = new AtomicReference<>();
        bus.suscribir(LatidosMundo.ChunkDescargado.TIPO, ev -> chunkDesc.set(new int[] {ev.chunkX(), ev.chunkZ()}));
        bus.emitir(LatidosMundo.ChunkDescargado.TIPO, new LatidosMundo.ChunkDescargado(MUNDO, -2, 7));
        assertThat(chunkDesc.get()).containsExactly(-2, 7);
    }

    @Test
    void tick_y_transicion_validan_valores() {
        assertThatThrownBy(() -> new LatidosMundo.Tick(MUNDO, -1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tickActual");

        assertThatThrownBy(() -> new LatidosMundo.NocheAmanece(
                MUNDO, 0L, 1L, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class MundoFalso implements Mundo {
        @Override
        public Identifier id() {
            return Identifier.of("vida", "test");
        }

        @Override
        public Dimension dimension() {
            return Dimension.OVERWORLD;
        }

        @Override
        public Bioma biomaEn(Coordenada coordenada) {
            return new Bioma(
                    Identifier.of("minecraft", "plains"),
                    0.8f,
                    0.4f,
                    Bioma.Precipitacion.LLUVIA);
        }

        @Override
        public boolean estaCargado(Coordenada coordenada) {
            return true;
        }

        @Override
        public long tiempoDelDia() {
            return 1000L;
        }
    }
}

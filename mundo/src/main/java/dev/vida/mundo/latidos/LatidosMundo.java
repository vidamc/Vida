/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo.latidos;

import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;
import dev.vida.mundo.Mundo;
import java.util.Objects;

/**
 * Набор стандартных world-латидосов: {@link MundoCargado}, {@link ChunkCargado}, {@link ChunkDescargado},
 * {@link Tick}, {@link NocheAmanece}.
 */
@ApiStatus.Stable
public final class LatidosMundo {

    private LatidosMundo() {}

    @ApiStatus.Stable
    public record MundoCargado(Mundo mundo, boolean recienCreado) {
        public static final Latido<MundoCargado> TIPO =
                Latido.de("vida:mundo_cargado", MundoCargado.class);

        public MundoCargado {
            Objects.requireNonNull(mundo, "mundo");
        }
    }

    @ApiStatus.Stable
    public record ChunkCargado(Mundo mundo, int chunkX, int chunkZ, boolean completo) {
        public static final Latido<ChunkCargado> TIPO =
                Latido.de("vida:chunk_cargado", ChunkCargado.class);

        public ChunkCargado {
            Objects.requireNonNull(mundo, "mundo");
        }
    }

    @ApiStatus.Stable
    public record ChunkDescargado(Mundo mundo, int chunkX, int chunkZ) {
        public static final Latido<ChunkDescargado> TIPO =
                Latido.de("vida:chunk_descargado", ChunkDescargado.class);

        public ChunkDescargado {
            Objects.requireNonNull(mundo, "mundo");
        }
    }

    @ApiStatus.Stable
    public record Tick(Mundo mundo, long tickActual, long tiempoDelDia) {
        public static final Latido<Tick> TIPO =
                Latido.de("vida:mundo_tick", Tick.class);

        public Tick {
            Objects.requireNonNull(mundo, "mundo");
            if (tickActual < 0L) {
                throw new IllegalArgumentException("tickActual < 0");
            }
            if (tiempoDelDia < 0L) {
                throw new IllegalArgumentException("tiempoDelDia < 0");
            }
        }
    }

    @ApiStatus.Stable
    public record NocheAmanece(
            Mundo mundo,
            long tiempoAnterior,
            long tiempoActual,
            Transicion transicion) {
        public static final Latido<NocheAmanece> TIPO =
                Latido.de("vida:noche_amanece", NocheAmanece.class);

        public NocheAmanece {
            Objects.requireNonNull(mundo, "mundo");
            Objects.requireNonNull(transicion, "transicion");
            if (tiempoAnterior < 0L) {
                throw new IllegalArgumentException("tiempoAnterior < 0");
            }
            if (tiempoActual < 0L) {
                throw new IllegalArgumentException("tiempoActual < 0");
            }
        }

        public enum Transicion {
            AMANECER,
            ANOCHECER
        }
    }
}

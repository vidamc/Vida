/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad.componentes;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;

/**
 * Общий маркер типизированного entity data-component.
 */
@ApiStatus.Stable
public sealed interface ComponenteEntidad permits
        ComponenteEntidad.Salud,
        ComponenteEntidad.VelocidadMovimiento,
        ComponenteEntidad.NombreVisible,
        ComponenteEntidad.Brillo,
        ComponenteEntidad.InmuneFuego,
        ComponenteEntidad.TablaBotin {

    record Salud(double actual, double maxima) implements ComponenteEntidad {
        public Salud {
            if (!Double.isFinite(maxima) || maxima <= 0d) {
                throw new IllegalArgumentException("maxima debe ser finita y > 0: " + maxima);
            }
            if (!Double.isFinite(actual) || actual < 0d || actual > maxima) {
                throw new IllegalArgumentException("actual вне [0.." + maxima + "]: " + actual);
            }
        }
    }

    record VelocidadMovimiento(double bloquesPorSegundo) implements ComponenteEntidad {
        public VelocidadMovimiento {
            if (!Double.isFinite(bloquesPorSegundo) || bloquesPorSegundo <= 0d) {
                throw new IllegalArgumentException(
                        "bloquesPorSegundo debe ser finito y > 0: " + bloquesPorSegundo);
            }
        }
    }

    record NombreVisible(String texto) implements ComponenteEntidad {
        public NombreVisible {
            if (texto == null || texto.isBlank()) {
                throw new IllegalArgumentException("texto blank");
            }
        }
    }

    record Brillo(boolean visible) implements ComponenteEntidad {}

    record InmuneFuego() implements ComponenteEntidad {}

    record TablaBotin(Identifier id) implements ComponenteEntidad {
        public TablaBotin {
            if (id == null) {
                throw new IllegalArgumentException("id null");
            }
        }
    }
}

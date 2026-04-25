/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cima;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Глобальная ссылка на Cima (аналогично идее {@code LatidoGlobal}: в
 * {@code :base#ModContext} зависимость <strong>vida-mundo</strong> не
 * протаскивали).
 */
@ApiStatus.Preview("cima")
public final class CimaJuegoGlobal {

    private static CimaJuego cimaJuego = CimaJuegoNulo.INSTANCIA;

    private CimaJuegoGlobal() {}

    public static CimaJuego cimaJuego() {
        return cimaJuego;
    }

    @ApiStatus.Internal
    public static void instalar(CimaJuego c) {
        cimaJuego = Objects.requireNonNull(c, "cimaJuego");
    }

    @ApiStatus.Internal
    public static void restaurarNuloSoloProbar() {
        cimaJuego = CimaJuegoNulo.INSTANCIA;
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Enlaza el drenado de {@link TejidoCanal} con el tick de juego (llamar una vez
 * por tick desde el hilo del juego).
 */
@ApiStatus.Stable
public final class TejidoTickEnlace {

    private TejidoTickEnlace() {}

    public static void enCadaTick(TejidoCanal canal, Consumer<List<TramaPaquete>> enviarFrames) {
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(enviarFrames, "enviarFrames");
        List<TramaPaquete> pendientes = canal.drenarPendientes();
        if (!pendientes.isEmpty()) {
            enviarFrames.accept(pendientes);
        }
    }
}

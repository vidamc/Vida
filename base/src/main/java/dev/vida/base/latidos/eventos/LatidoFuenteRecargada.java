/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos.eventos;

import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;
import java.time.Instant;
import java.util.Objects;

/** Игровые ресурсы / datapack перезагружены; моды с Fuente должны обновить кэши. */
@ApiStatus.Stable
public record LatidoFuenteRecargada(Instant momento, OrigenRecargaAjustes origen) {

    public LatidoFuenteRecargada {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(origen, "origen");
    }

    public static final Latido<LatidoFuenteRecargada> TIPO =
            Latido.de("vida:fuente_recargada", LatidoFuenteRecargada.class);
}

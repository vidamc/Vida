/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos.eventos;

import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Настройки были перечитаны с диска или с сети; подписчики должны обновить кэши.
 */
@ApiStatus.Stable
public record LatidoConfiguracionRecargada(Instant momento, OrigenRecargaAjustes origen) {

    public LatidoConfiguracionRecargada {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(origen, "origen");
    }

    public static final Latido<LatidoConfiguracionRecargada> TIPO =
            Latido.de("vida:configuracion_recargada", LatidoConfiguracionRecargada.class);
}

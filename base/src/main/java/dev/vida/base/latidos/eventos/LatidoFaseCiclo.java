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
 * Глобальная фаза бутстрапа модов (не привязана к одному mod id).
 *
 * @param momento   момент эмиссии
 * @param fase      какая фаза началась
 * @param modsTotal число модов в текущей резолюции (для телеметрии/логов)
 */
@ApiStatus.Stable
public record LatidoFaseCiclo(Instant momento, FaseCicloMod fase, int modsTotal) {

    public LatidoFaseCiclo {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(fase, "fase");
        if (modsTotal < 0) {
            throw new IllegalArgumentException("modsTotal < 0");
        }
    }

    public static final Latido<LatidoFaseCiclo> TIPO =
            Latido.de("vida:fase_ciclo_mod", LatidoFaseCiclo.class);
}

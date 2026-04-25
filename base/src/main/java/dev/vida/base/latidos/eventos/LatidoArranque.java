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
 * Событие «Vida полностью поднялась и моды готовы к работе».
 *
 * <p>Диспатчится ровно один раз за жизнь процесса после того, как все
 * {@link dev.vida.base.VidaMod#iniciar} отработали без падений. Моды могут
 * использовать его для операций, требующих полного реестра других модов
 * (например, поиска опциональных интеграций).
 */
@ApiStatus.Stable
public record LatidoArranque(Instant momento, int modsCargados) {

    public LatidoArranque {
        Objects.requireNonNull(momento, "momento");
        if (modsCargados < 0) throw new IllegalArgumentException("modsCargados < 0");
    }

    public static final Latido<LatidoArranque> TIPO =
            Latido.de("vida:arranque", LatidoArranque.class);
}

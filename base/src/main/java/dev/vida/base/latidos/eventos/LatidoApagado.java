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
 * Событие выключения рантайма: диспатчится <b>до</b> фактической выгрузки
 * модов. Моды должны успеть сохранить состояние и отписаться от внешних
 * ресурсов.
 *
 * @param momento    время, когда сигнал был инициирован
 * @param razon      текстовое описание причины выключения
 */
@ApiStatus.Stable
public record LatidoApagado(Instant momento, String razon) {

    public LatidoApagado {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(razon, "razon");
    }

    public static final Latido<LatidoApagado> TIPO =
            Latido.de("vida:apagado", LatidoApagado.class);
}

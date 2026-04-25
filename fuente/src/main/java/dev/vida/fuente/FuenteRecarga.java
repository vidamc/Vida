/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.eventos.LatidoFuenteRecargada;
import dev.vida.base.latidos.eventos.OrigenRecargaAjustes;
import dev.vida.core.ApiStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Подписка на {@link LatidoFuenteRecargada} для сброса кэшей data-driven контента.
 */
@ApiStatus.Stable
public final class FuenteRecarga {

    private FuenteRecarga() {}

    /**
     * Регистрирует обработчик, вызываемый при каждой перезагрузке ресурсов (идемпотентен:
     * можно вызывать несколько раз — каждый раз добавится подписчик).
     */
    public static void alRecargar(LatidoBus bus, Runnable accion) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(accion, "accion");
        bus.suscribir(LatidoFuenteRecargada.TIPO, ev -> accion.run());
    }

    /** Публикует событие (серверный код или тесты). */
    public static void emitir(LatidoBus bus, OrigenRecargaAjustes origen) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(origen, "origen");
        bus.emitir(LatidoFuenteRecargada.TIPO, new LatidoFuenteRecargada(Instant.now(), origen));
    }
}

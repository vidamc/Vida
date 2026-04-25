/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos.eventos;

import dev.vida.core.ApiStatus;

/** Источник сигнала перезагрузки настроек / ресурсов. */
@ApiStatus.Stable
public enum OrigenRecargaAjustes {
    /** Игрок или игра перезагрузили ресурсы (например F3+T на клиенте). */
    RECURSOS,
    /** Явная команда или вызов API. */
    COMANDO,
    /** Снимок с сервера (см. Tejido {@code PaqueteAjustesSincronizacionServidor}). */
    RED
}

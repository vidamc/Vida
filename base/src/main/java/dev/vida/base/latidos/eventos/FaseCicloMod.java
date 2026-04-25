/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos.eventos;

import dev.vida.core.ApiStatus;

/**
 * Упорядоченные фазы жизненного цикла набора модов при бутстрапе (см. {@link LatidoFaseCiclo}).
 *
 * <p>Порядок эмиссии глобальных фаз: {@link #PREPARACION} → {@link #INICIALIZACION} →
 * {@link #POST_INICIALIZACION}. После entrypoints следует {@link LatidoArranque}.
 */
@ApiStatus.Stable
public enum FaseCicloMod {
    /** Перед любыми entrypoints: только подписки и лёгкая подготовка. */
    PREPARACION,
    /** Перед {@code main} entrypoints (после {@code preLaunch}). */
    INICIALIZACION,
    /** После {@code main}, перед {@code server} и отложенным {@code client}. */
    POST_INICIALIZACION
}

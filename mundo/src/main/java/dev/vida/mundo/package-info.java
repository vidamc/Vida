/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный world API Vida.
 *
 * <p>Value-types мира ({@link dev.vida.mundo.Coordenada}, {@link dev.vida.mundo.ChunkCoordenada},
 * {@link dev.vida.mundo.RegionCoordenada}, {@link dev.vida.mundo.LimitesVerticales},
 * {@link dev.vida.mundo.Dimension}, {@link dev.vida.mundo.Bioma}) и интерфейс {@link dev.vida.mundo.Mundo} не зависят от vanilla
 * runtime. События уровня мира проходят через {@link dev.vida.mundo.latidos.LatidosMundo} и
 * общую шину {@link dev.vida.base.latidos.LatidoBus} из {@code vida-base}.
 *
 * <p>Сетевой обмен — модуль {@code vida-red}; клиентский рендер — {@code vida-render}.
 */
@ApiStatus.Stable
package dev.vida.mundo;

import dev.vida.core.ApiStatus;

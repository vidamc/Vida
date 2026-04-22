/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Ядро render-pipeline Valenta: compact vertex format, GPU-буферы,
 * multi-draw indirect batching и SSBO-backed данные.
 *
 * <p>Все классы этого пакета оперируют на уровне OpenGL 4.3+ и
 * предполагают вызов строго из render-потока, если не указано иное.
 */
@dev.vida.core.ApiStatus.Preview("valenta")
package dev.vida.mods.valenta.core;

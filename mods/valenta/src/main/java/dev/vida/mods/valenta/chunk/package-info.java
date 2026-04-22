/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Chunk meshing pipeline: task-graph через {@link dev.vida.susurro.Susurro}
 * с этапами Analisis → Build → Upload.
 *
 * <p>Все задачи помечены {@code Etiqueta.de("valenta/chunk")} для
 * back-pressure; пул никогда не блокирует render-поток.
 */
@dev.vida.core.ApiStatus.Preview("valenta")
package dev.vida.mods.valenta.chunk;

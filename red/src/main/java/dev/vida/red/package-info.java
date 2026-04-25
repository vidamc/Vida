/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный сетевой API Vida (контур <em>Tejido</em> в терминологии проекта):
 * версионируемые пакеты, record-codecs и back-pressure.
 *
 * <p>Отвечает за канал клиент↔сервер; игровая модель мира остаётся в {@code vida-mundo},
 * точки входа мода и события — в {@code vida-base}.
 */
@ApiStatus.Stable
package dev.vida.red;

import dev.vida.core.ApiStatus;

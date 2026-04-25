/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный render API Vida: декларативные модели блоков и сущностей, texture atlas
 * и pipeline-хуки шейдеров.
 *
 * <p>Оверлей через мост сейчас ориентирован на примитивы (прямоугольник, текст);
 * спрайты, слои HUD и «набор кирпичиков» для сложных панелей — расширяемая зона API.
 *
 * <p>Клиент-oriented слой: используется вместе с {@code vida-base} и платформенным мостом
 * загрузчика. Там, где контент уже описан в {@code vida-bloque} / {@code vida-entidad},
 * регистрация согласуется с {@link dev.vida.base.catalogo.CatalogoManejador}.
 */
@ApiStatus.Stable
package dev.vida.render;

import dev.vida.core.ApiStatus;

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный API Vida для сущностей Minecraft.
 *
 * <p>Модуль описывает entity-type декларативно: тип, hitbox, масса,
 * AI-группы и data-components. Конкретная интеграция с runtime-миром
 * живёт в {@code vida-mundo}, поэтому пакет остаётся side-agnostic и
 * пригодным для обычных unit-тестов.
 */
@ApiStatus.Preview("entidad")
package dev.vida.entidad;

import dev.vida.core.ApiStatus;

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный API Vida для сущностей Minecraft.
 *
 * <p>Модуль описывает entity-type декларативно: тип, hitbox, масса,
 * AI-группы и data-components. Регистрация строится поверх
 * {@link dev.vida.base.catalogo.CatalogoManejador} через
 * {@link dev.vida.entidad.registro.RegistroEntidades} — по тому же принципу, что
 * регистрация в модуле {@code vida-bloque}.
 *
 * <p>Интеграция с runtime-миром и тиками — в {@code vida-mundo}; пакет остаётся
 * side-agnostic и пригодным для обычных unit-тестов.
 */
@ApiStatus.Stable
package dev.vida.entidad;

import dev.vida.core.ApiStatus;

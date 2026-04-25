/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный API Vida для блоков Minecraft.
 *
 * <p>Модуль пока side-agnostic: модель блока описывается декларативно
 * ({@link dev.vida.bloque.PropiedadesBloque}), а конкретная интеграция с
 * rendering/физикой vanilla подъедет в {@code vida-mundo} / {@code vida-render}.
 * Благодаря этому модуль можно тестировать в обычных unit-тестах и
 * переиспользовать на сервере.
 *
 * <p>Регистрация идёт через
 * {@link dev.vida.bloque.registro.RegistroBloques}, который строится
 * поверх общего {@link dev.vida.base.catalogo.CatalogoManejador}.
 */
@ApiStatus.Stable
package dev.vida.bloque;

import dev.vida.core.ApiStatus;

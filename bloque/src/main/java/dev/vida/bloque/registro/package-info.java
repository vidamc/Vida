/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Регистрация блоков в {@link dev.vida.base.catalogo.CatalogoManejador}.
 *
 * <p>Главный вход — {@link dev.vida.bloque.registro.RegistroBloques}. Он
 * оборачивает общий {@code CatalogoMutable<Bloque>} и добавляет
 * тип-специфичные помощники (регистрация блока и tagging).
 */
@ApiStatus.Stable
package dev.vida.bloque.registro;

import dev.vida.core.ApiStatus;

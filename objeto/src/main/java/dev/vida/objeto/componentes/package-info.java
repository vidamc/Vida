/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Типизированные data-компоненты предметов (Minecraft 1.21.1).
 *
 * <p>В 1.20.5 vanilla перешла от флагов на NBT к явным компонентам
 * {@code DataComponentType<T>}. Vida отражает эту модель на Java-типах:
 * каждый компонент — {@code record}, {@link dev.vida.objeto.componentes.Componente}
 * — общий sealed-интерфейс, {@link dev.vida.objeto.componentes.ClaveComponente}
 * — типизированный ключ.
 *
 * <p>Набор компонентов хранится в
 * {@link dev.vida.objeto.componentes.MapaComponentes} — иммутабельная
 * type-safe карта, заменяющая vanilla {@code DataComponentMap}.
 */
@ApiStatus.Stable
package dev.vida.objeto.componentes;

import dev.vida.core.ApiStatus;

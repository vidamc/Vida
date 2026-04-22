/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;

/**
 * Действие, которое производит {@link PuertaDirectiva} над целевым членом.
 *
 * <ul>
 *   <li>{@link #ACCESIBLE} — сделать {@code public}. Для полей дополнительно
 *       снимается {@code final} (семантика Fabric AW);</li>
 *   <li>{@link #EXTENSIBLE} — снять {@code final} с класса (позволяет наследование)
 *       или с метода (позволяет override);</li>
 *   <li>{@link #MUTABLE} — снять {@code final} с поля, доступность не меняет.</li>
 * </ul>
 */
@ApiStatus.Preview("puertas")
public enum Accion {
    ACCESIBLE,
    EXTENSIBLE,
    MUTABLE
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;

/**
 * Категория целевого члена директивы.
 */
@ApiStatus.Stable
public enum Objetivo {
    CLASE,
    METODO,
    CAMPO
}

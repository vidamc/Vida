/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;

/**
 * Категория целевого члена директивы.
 */
@ApiStatus.Preview("puertas")
public enum Objetivo {
    CLASE,
    METODO,
    CAMPO
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;

/**
 * Направление движения пакета.
 */
@ApiStatus.Preview("red")
public enum DireccionPaquete {
    CLIENTE_A_SERVIDOR,
    SERVIDOR_A_CLIENTE
}

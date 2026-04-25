/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.core.ApiStatus;

/**
 * Контекст вызова shader-hook в render pipeline.
 */
@ApiStatus.Stable
public record ContextoShader(long frameId, long tiempoNanos, ShaderHook.Etapa etapa) {

    public ContextoShader {
        if (frameId < 0L) {
            throw new IllegalArgumentException("frameId < 0");
        }
        if (tiempoNanos < 0L) {
            throw new IllegalArgumentException("tiempoNanos < 0");
        }
    }
}

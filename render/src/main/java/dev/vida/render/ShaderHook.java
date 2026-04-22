/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.core.ApiStatus;

/**
 * Hook render-pipeline для вставки шейдерного кода.
 */
@ApiStatus.Preview("render")
@FunctionalInterface
public interface ShaderHook {

    void aplicar(ContextoShader contexto);

    enum Etapa {
        ANTES_MUNDO,
        DESPUES_MUNDO,
        HUD
    }
}

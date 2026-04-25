/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;

/**
 * Привязка параметра инъектора к локальной переменной целевого метода через LVT.
 *
 * @param parameterIndex индекс параметра инъектора (0-based, без учёта CallbackInfo)
 * @param ordinal        какую по счёту подходящую переменную взять в LVT на точке инъекции
 * @param descriptor     ограничение JVM-типа локальной переменной ({@code ""} — любой)
 */
@ApiStatus.Internal
public record VifadaLocalBinding(int parameterIndex, int ordinal, String descriptor) {

    public VifadaLocalBinding {
        if (parameterIndex < 0) {
            throw new IllegalArgumentException("parameterIndex must be >= 0");
        }
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be >= 0");
        }
        descriptor = descriptor == null ? "" : descriptor;
    }
}

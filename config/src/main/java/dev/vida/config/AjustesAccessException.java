/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Unchecked-обёртка для strict-геттеров {@link Ajustes}.
 *
 * <p>Содержит типизированную причину через {@link #error()}, что позволяет
 * вызывающему коду обрабатывать ошибку аналогично ветке {@code Result.Err}.
 */
@ApiStatus.Stable
public final class AjustesAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final AjustesError error;

    public AjustesAccessException(AjustesError error) {
        super(Objects.requireNonNull(error, "error").message());
        this.error = error;
    }

    public AjustesError error() { return error; }
}

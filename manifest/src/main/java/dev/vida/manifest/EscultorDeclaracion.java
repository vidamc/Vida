/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Объявление класса Escultor (байткод-патчер) в {@code vida.mod.json}.
 *
 * @param className полное имя класса (FQN)
 * @param priority меньшее значение — раньше в цепочке трансформации относительно других Escultor
 */
@ApiStatus.Stable
public record EscultorDeclaracion(String className, int priority) {

    public EscultorDeclaracion {
        Objects.requireNonNull(className, "className");
        if (className.isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }
    }

    public static EscultorDeclaracion of(String className) {
        return new EscultorDeclaracion(className, 0);
    }
}

/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.core.ApiStatus;
import java.util.Optional;

/**
 * Валидатор значения настройки.
 *
 * <p>Возвращает пустой {@link Optional}, если значение допустимо; иначе —
 * текстовое описание ошибки.
 *
 * @param <T> тип значения
 */
@FunctionalInterface
@ApiStatus.Stable
public interface Validador<T> {

    Optional<String> validar(T valor);

    /** Комбинирует два валидатора: пробегает сначала this, затем other. */
    default Validador<T> y(Validador<T> other) {
        return v -> {
            Optional<String> a = validar(v);
            return a.isPresent() ? a : other.validar(v);
        };
    }

    /** Валидатор «всегда ок». */
    static <T> Validador<T> ninguno() { return v -> Optional.empty(); }
}

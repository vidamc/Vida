/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Строковая метка, связывающая задачу с источником.
 *
 * <p>Используется для back-pressure: {@link Susurro.Politica} позволяет
 * указать лимит одновременно исполняющихся задач на метку. Типично —
 * по одной метке на мод ({@code "example-mod/ai"},
 * {@code "example-mod/io"}), чтобы одна система не могла вытеснить
 * другую из пула.
 */
@ApiStatus.Preview("susurro")
public record Etiqueta(String valor) {

    public Etiqueta {
        Objects.requireNonNull(valor, "valor");
        if (valor.isBlank()) {
            throw new IllegalArgumentException("etiqueta пустая");
        }
        if (valor.length() > 128) {
            throw new IllegalArgumentException("etiqueta длиннее 128 символов");
        }
    }

    public static Etiqueta de(String valor) { return new Etiqueta(valor); }
}

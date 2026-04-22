/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/** Структурированные ошибки реестров. */
@ApiStatus.Preview("base")
public sealed interface CatalogoError {

    /** Попытка записать значение под уже занятым ключом. */
    record ClaveDuplicada(CatalogoClave<?> clave, String existente) implements CatalogoError {
        public ClaveDuplicada {
            Objects.requireNonNull(clave, "clave");
            Objects.requireNonNull(existente, "existente");
        }
    }

    /** Реестр уже «заморожен» (фаза регистрации окончена). */
    record CatalogoCerrado(String reestroId) implements CatalogoError {
        public CatalogoCerrado { Objects.requireNonNull(reestroId, "reestroId"); }
    }

    /** Ключ относится к другому реестру. */
    record ClaveAjena(String esperado, String recibido) implements CatalogoError {
        public ClaveAjena {
            Objects.requireNonNull(esperado, "esperado");
            Objects.requireNonNull(recibido, "recibido");
        }
    }

    /** Запрошенного ключа нет. */
    record NoEncontrado(CatalogoClave<?> clave) implements CatalogoError {
        public NoEncontrado { Objects.requireNonNull(clave, "clave"); }
    }
}

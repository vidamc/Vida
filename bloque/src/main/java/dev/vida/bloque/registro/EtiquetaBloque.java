/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque.registro;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Типизированный идентификатор тега блоков.
 *
 * <p>Аналог vanilla {@code TagKey<Block>}: позволяет ссылаться на набор
 * блоков по имени, а не по фиксированному списку (как Vida-tag
 * {@code vida:mineable/pico} — всё, что ломается киркой).
 *
 * <p>Само содержимое тега держится в {@link RegistroBloques} и может
 * пополняться модами.
 */
@ApiStatus.Preview("bloque")
public record EtiquetaBloque(Identifier id) {

    public EtiquetaBloque {
        Objects.requireNonNull(id, "id");
    }

    public static EtiquetaBloque de(String id) {
        return new EtiquetaBloque(Identifier.parse(id));
    }

    public static EtiquetaBloque de(String namespace, String path) {
        return new EtiquetaBloque(Identifier.of(namespace, path));
    }
}

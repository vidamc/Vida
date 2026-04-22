/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos.eventos;

import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Событие регистрации нового элемента в {@link dev.vida.base.catalogo.Catalogo}.
 *
 * <p>Диспатчится после того, как запись успешно сохранена в реестре.
 * Подписчики могут использовать это событие, чтобы строить обратные
 * индексы, локализационные словари, сетевые справочники.
 *
 * @param reestroId   id реестра ({@code vida:block}, {@code ejemplo:herramienta})
 * @param clave       ключ записи
 * @param valor       зарегистрированное значение (generic Object, чтобы не
 *                    привязываться к параметру type реестра на уровне шины)
 */
@ApiStatus.Preview("base")
public record LatidoRegistro(String reestroId, CatalogoClave<?> clave, Object valor) {

    public LatidoRegistro {
        Objects.requireNonNull(reestroId, "reestroId");
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(valor, "valor");
    }

    public static final Latido<LatidoRegistro> TIPO =
            Latido.de("vida:catalogo/registro", LatidoRegistro.class);
}

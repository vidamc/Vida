/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Блок с привязанной {@link ContextoBloqueEntidad}.
 *
 * <p>В vanilla BlockEntity хранит per-instance state (например, содержимое
 * сундука или текст таблички). Vida делает аналогичную конструкцию
 * side-agnostic: {@link ContextoBloqueEntidad} — это произвольный контекст
 * мода, а bridge к vanilla NBT/DataComponents живёт в будущем
 * {@code vida-mundo}.
 *
 * <p>Фабрика контекста вызывается один раз на каждую установленную позицию
 * в мире; её результат — уникальный объект, принадлежащий этой позиции.
 *
 * @param <C> тип контекста BlockEntity
 */
@ApiStatus.Stable
public class BloqueEntidad<C extends ContextoBloqueEntidad> extends Bloque {

    private final Supplier<C> fabrica;

    public BloqueEntidad(Identifier id, PropiedadesBloque propiedades, Supplier<C> fabrica) {
        super(id, propiedades);
        this.fabrica = Objects.requireNonNull(fabrica, "fabrica");
    }

    /** Создаёт новый контекст для свежеустановленной позиции. */
    public C crearContexto() {
        C c = fabrica.get();
        if (c == null) {
            throw new IllegalStateException(
                    "fabrica для " + id() + " вернула null");
        }
        return c;
    }

    /** Фабрика контекста (для повторного использования, например на client-копиях). */
    public Supplier<C> fabrica() { return fabrica; }
}

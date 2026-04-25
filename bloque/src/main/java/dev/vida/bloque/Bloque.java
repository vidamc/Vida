/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Блок Vida.
 *
 * <p>Инстанс связывает {@link Identifier} (уникальный ключ в
 * {@link dev.vida.bloque.registro.RegistroBloques}) и
 * {@link PropiedadesBloque} (все физические/игровые свойства). Это
 * data-объект — никакой mutable state, никакой vanilla-зависимости.
 *
 * <p>Более сложное поведение реализуется через подклассы (например,
 * {@link dev.vida.bloque.BloqueEntidad} добавляет BlockEntity-контекст).
 * Для большинства пользовательских блоков подкласс не нужен — достаточно
 * {@link PropiedadesBloque} + запись в {@code Catalogo}.
 */
@ApiStatus.Stable
public class Bloque {

    private final Identifier id;
    private final PropiedadesBloque propiedades;

    /**
     * Конструктор.
     *
     * @param id           уникальный идентификатор блока
     * @param propiedades  свойства
     */
    public Bloque(Identifier id, PropiedadesBloque propiedades) {
        this.id = Objects.requireNonNull(id, "id");
        this.propiedades = Objects.requireNonNull(propiedades, "propiedades");
    }

    /** Уникальный идентификатор блока. */
    public final Identifier id() { return id; }

    /** Свойства блока. */
    public final PropiedadesBloque propiedades() { return propiedades; }

    /**
     * Удобный shortcut: материал блока.
     */
    public final MaterialBloque material() { return propiedades.material(); }

    /**
     * Удобный shortcut: форма коллизии.
     */
    public final FormaColision forma() { return propiedades.forma(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bloque other = (Bloque) o;
        return id.equals(other.id) && propiedades.equals(other.propiedades);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + propiedades.hashCode();
    }

    @Override
    public String toString() {
        return "Bloque(" + id + ", " + propiedades.material() + ")";
    }
}

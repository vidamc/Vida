/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Предмет Vida.
 *
 * <p>Data-объект: {@link Identifier} + {@link PropiedadesObjeto}. Не имеет
 * mutable state и никак не завязан на конкретный side. Для предметов,
 * представляющих блок, используйте {@link ObjetoDeBloque}.
 */
@ApiStatus.Stable
public class Objeto {

    private final Identifier id;
    private final PropiedadesObjeto propiedades;

    public Objeto(Identifier id, PropiedadesObjeto propiedades) {
        this.id = Objects.requireNonNull(id, "id");
        this.propiedades = Objects.requireNonNull(propiedades, "propiedades");
    }

    public final Identifier id() { return id; }
    public final PropiedadesObjeto propiedades() { return propiedades; }

    /** Shortcut: редкость. */
    public final Raridad raridad() { return propiedades.raridad(); }

    /** Shortcut: тип. */
    public final TipoObjeto tipo() { return propiedades.tipo(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Objeto other = (Objeto) o;
        return id.equals(other.id) && propiedades.equals(other.propiedades);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + propiedades.hashCode();
    }

    @Override
    public String toString() {
        return "Objeto(" + id + ", " + propiedades.tipo() + ")";
    }
}

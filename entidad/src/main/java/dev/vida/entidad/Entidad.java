/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.entidad.componentes.MapaComponentesEntidad;
import java.util.Objects;

/**
 * Декларативное описание entity-type в Vida.
 *
 * <p>Содержит только стабильные, сериализуемые характеристики типа:
 * идентификатор, базовую категорию и иммутабельные свойства.
 */
@ApiStatus.Stable
public final class Entidad {

    private final Identifier id;
    private final TipoEntidad tipo;
    private final PropiedadesEntidad propiedades;

    public Entidad(Identifier id, TipoEntidad tipo, PropiedadesEntidad propiedades) {
        this.id = Objects.requireNonNull(id, "id");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.propiedades = Objects.requireNonNull(propiedades, "propiedades");
    }

    public Identifier id() { return id; }
    public TipoEntidad tipo() { return tipo; }
    public PropiedadesEntidad propiedades() { return propiedades; }

    /** Удобный shortcut к массе сущности. */
    public double masa() { return propiedades.masa(); }

    /** Удобный shortcut к hitbox'у сущности. */
    public PropiedadesEntidad.Hitbox hitbox() { return propiedades.hitbox(); }

    /** Удобный shortcut к data-components сущности. */
    public MapaComponentesEntidad componentes() { return propiedades.componentes(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entidad other = (Entidad) o;
        return id.equals(other.id)
                && tipo == other.tipo
                && propiedades.equals(other.propiedades);
    }

    @Override
    public int hashCode() {
        return ((id.hashCode() * 31) + tipo.hashCode()) * 31 + propiedades.hashCode();
    }

    @Override
    public String toString() {
        return "Entidad(" + id + ", " + tipo + ")";
    }
}

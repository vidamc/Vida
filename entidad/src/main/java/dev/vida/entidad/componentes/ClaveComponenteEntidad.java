/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad.componentes;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Типизированный ключ entity data-component.
 *
 * @param <T> фактический тип компонента
 */
@ApiStatus.Preview("entidad")
public final class ClaveComponenteEntidad<T extends ComponenteEntidad> {

    private final Identifier id;
    private final Class<T> tipo;

    private ClaveComponenteEntidad(Identifier id, Class<T> tipo) {
        this.id = Objects.requireNonNull(id, "id");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
    }

    public Identifier id() { return id; }
    public Class<T> tipo() { return tipo; }

    public static <T extends ComponenteEntidad> ClaveComponenteEntidad<T> de(Identifier id, Class<T> tipo) {
        return new ClaveComponenteEntidad<>(id, tipo);
    }

    public static <T extends ComponenteEntidad> ClaveComponenteEntidad<T> de(String id, Class<T> tipo) {
        return new ClaveComponenteEntidad<>(Identifier.parse(id), tipo);
    }

    public static final ClaveComponenteEntidad<ComponenteEntidad.Salud> SALUD =
            de("minecraft:max_health", ComponenteEntidad.Salud.class);

    public static final ClaveComponenteEntidad<ComponenteEntidad.VelocidadMovimiento> VELOCIDAD =
            de("minecraft:movement_speed", ComponenteEntidad.VelocidadMovimiento.class);

    public static final ClaveComponenteEntidad<ComponenteEntidad.NombreVisible> NOMBRE_VISIBLE =
            de("minecraft:custom_name", ComponenteEntidad.NombreVisible.class);

    public static final ClaveComponenteEntidad<ComponenteEntidad.Brillo> BRILLO =
            de("minecraft:glowing", ComponenteEntidad.Brillo.class);

    public static final ClaveComponenteEntidad<ComponenteEntidad.InmuneFuego> INMUNE_FUEGO =
            de("minecraft:fire_resistant", ComponenteEntidad.InmuneFuego.class);

    public static final ClaveComponenteEntidad<ComponenteEntidad.TablaBotin> TABLA_BOTIN =
            de("minecraft:loot_table", ComponenteEntidad.TablaBotin.class);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaveComponenteEntidad<?> other)) return false;
        return id.equals(other.id) && tipo.equals(other.tipo);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + tipo.hashCode();
    }

    @Override
    public String toString() {
        return "ClaveComponenteEntidad(" + id + " : " + tipo.getSimpleName() + ")";
    }
}

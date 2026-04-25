/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad.componentes;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Типизированная неизменяемая карта entity data-components.
 */
@ApiStatus.Stable
public final class MapaComponentesEntidad {

    private static final MapaComponentesEntidad VACIO = new MapaComponentesEntidad(Map.of());

    private final Map<Identifier, ComponenteEntidad> porId;

    private MapaComponentesEntidad(Map<Identifier, ComponenteEntidad> porId) {
        this.porId = porId;
    }

    public static MapaComponentesEntidad vacio() { return VACIO; }
    public static Constructor con() { return new Constructor(); }

    public int tamanio() { return porId.size(); }
    public boolean esVacio() { return porId.isEmpty(); }

    public boolean contiene(ClaveComponenteEntidad<?> clave) {
        Objects.requireNonNull(clave, "clave");
        ComponenteEntidad valor = porId.get(clave.id());
        return valor != null && clave.tipo().isInstance(valor);
    }

    @SuppressWarnings("unchecked")
    public <T extends ComponenteEntidad> Optional<T> obtener(ClaveComponenteEntidad<T> clave) {
        Objects.requireNonNull(clave, "clave");
        ComponenteEntidad valor = porId.get(clave.id());
        if (valor == null) return Optional.empty();
        if (!clave.tipo().isInstance(valor)) return Optional.empty();
        return Optional.of((T) valor);
    }

    public Collection<ComponenteEntidad> valores() { return porId.values(); }
    public Collection<Identifier> ids() { return porId.keySet(); }

    public MapaComponentesEntidad fusionar(MapaComponentesEntidad otra) {
        Objects.requireNonNull(otra, "otra");
        if (otra.esVacio()) return this;
        Map<Identifier, ComponenteEntidad> copia = new LinkedHashMap<>(this.porId);
        copia.putAll(otra.porId);
        return new MapaComponentesEntidad(Map.copyOf(copia));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapaComponentesEntidad other)) return false;
        return porId.equals(other.porId);
    }

    @Override
    public int hashCode() {
        return porId.hashCode();
    }

    @Override
    public String toString() {
        return "MapaComponentesEntidad" + porId;
    }

    /** Fluent-билдер. */
    public static final class Constructor {
        private final Map<Identifier, ComponenteEntidad> buf = new LinkedHashMap<>();

        Constructor() {}

        public <T extends ComponenteEntidad> Constructor poner(ClaveComponenteEntidad<T> clave, T valor) {
            Objects.requireNonNull(clave, "clave");
            Objects.requireNonNull(valor, "valor");
            if (!clave.tipo().isInstance(valor)) {
                throw new ClassCastException(
                        "значение " + valor.getClass().getName()
                                + " не является " + clave.tipo().getName());
            }
            buf.put(clave.id(), valor);
            return this;
        }

        public Constructor quitar(ClaveComponenteEntidad<?> clave) {
            Objects.requireNonNull(clave, "clave");
            buf.remove(clave.id());
            return this;
        }

        public Constructor ponerTodo(MapaComponentesEntidad mapa) {
            Objects.requireNonNull(mapa, "mapa");
            buf.putAll(mapa.porId);
            return this;
        }

        public MapaComponentesEntidad construir() {
            if (buf.isEmpty()) return vacio();
            return new MapaComponentesEntidad(Map.copyOf(buf));
        }
    }
}
